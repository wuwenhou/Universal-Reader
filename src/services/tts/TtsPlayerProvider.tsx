import React, {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
} from 'react';

import {
  Tts,
  TtsPlaybackState,
  TtsProgress,
  TtsSession,
  TtsSettings,
} from '@modules/nitro-tts';
import { askForPostNotificationsPermission } from '@utils/askForPostNoftificationsPermission';

export type TtsCommand =
  | 'next'
  | 'pause'
  | 'play'
  | 'previous'
  | 'replay'
  | 'stop';

export interface TtsPlayerChapter {
  /** Title of the novel. */
  novelName: string;
  /** Title of the chapter being read. */
  chapterName: string;
  /** Cover image of the novel. */
  coverUri?: string;
  /** Chapter id, used to detect a chapter change. */
  chapterId?: number;
  /** Id of the novel the chapter belongs to. */
  novelId?: number;
  /** Id of the plugin the novel came from. */
  pluginId?: string;
}

export interface TtsPlayerState {
  /** Native playback state. */
  state: TtsPlaybackState;
  /** Progress inside the active queue. */
  progress: TtsProgress;
  /** The sentence that is currently spoken. */
  currentSentence: string;
  /** Every sentence of the active queue. */
  sentences: string[];
  /** Metadata of the chapter being read. */
  metadata?: TtsPlayerChapter;
  /** Last error reported by the native engine. */
  error: string | null;
  /** Minutes left on the sleep timer, `0` when no timer runs. */
  sleepTimerMinutes: number;
  /** Whether a session is loaded and can be controlled. */
  hasQueue: boolean;
}

export interface TtsPlayerActions {
  /** Loads a queue and starts playing it. */
  loadAndPlay: (
    queue: string[],
    startIndex: number,
    metadata: TtsPlayerChapter,
    settings: TtsSettings,
  ) => Promise<void>;
  /** Runs a transport command. */
  command: (command: TtsCommand) => void;
  /** Seeks to a sentence. */
  seekTo: (index: number) => void;
  /** Applies new voice settings. */
  updateSettings: (settings: TtsSettings) => void;
  /** Sets (or with `0` cancels) the sleep timer. */
  setSleepTimer: (minutes: number) => void;
  /** Stops playback and clears the queue. */
  stop: () => void;
}

type TtsPlayerContextValue = TtsPlayerState & TtsPlayerActions;

const initialProgress: TtsProgress = {
  index: 0,
  total: 0,
  paragraphId: '',
};

const initialState: TtsPlayerState = {
  state: 'idle',
  progress: initialProgress,
  currentSentence: '',
  sentences: [],
  metadata: undefined,
  error: null,
  sleepTimerMinutes: 0,
  hasQueue: false,
};

const TtsPlayerContext = createContext<TtsPlayerContextValue>({
  ...initialState,
  loadAndPlay: async () => {},
  command: () => {},
  seekTo: () => {},
  updateSettings: () => {},
  setSleepTimer: () => {},
  stop: () => {},
});

/**
 * Owns the native text-to-speech session for the whole app.
 *
 * The reader used to own the session, which meant playback stopped as soon
 * as the reader was left. The session lives here instead, so the listening
 * player keeps playing while the user browses the library.
 */
export const TtsPlayerProvider = ({
  children,
}: {
  children: React.ReactNode;
}) => {
  const sessionRef = useRef<TtsSession | null>(null);
  const sessionPromiseRef = useRef<Promise<TtsSession> | null>(null);
  const subscriptionsRef = useRef<{ remove(): void }[]>([]);
  const mountedRef = useRef(true);
  const sleepTimerRef = useRef<ReturnType<typeof setInterval> | null>(null);

  const [state, setState] = useState<TtsPlaybackState>('idle');
  const [progress, setProgress] = useState<TtsProgress>(initialProgress);
  const [sentences, setSentences] = useState<string[]>([]);
  const [metadata, setMetadata] = useState<TtsPlayerChapter | undefined>();
  const [error, setError] = useState<string | null>(null);
  const [sleepTimerMinutes, setSleepTimerMinutes] = useState(0);

  const ensureSession = useCallback(async () => {
    if (sessionRef.current) {
      return sessionRef.current;
    }
    if (!sessionPromiseRef.current) {
      sessionPromiseRef.current = Tts.createSession()
        .then(session => {
          if (!mountedRef.current) {
            void session.stop();
            return session;
          }
          sessionRef.current = session;
          subscriptionsRef.current = [
            session.addOnStateChangedListener(setState),
            session.addOnProgressChangedListener(setProgress),
            session.addOnErrorListener(setError),
          ];
          return session;
        })
        .catch(cause => {
          sessionPromiseRef.current = null;
          throw cause;
        });
    }
    return sessionPromiseRef.current;
  }, []);

  const run = useCallback(
    async (operation: (session: TtsSession) => Promise<void>) => {
      try {
        setError(null);
        await operation(await ensureSession());
      } catch (cause) {
        setError(cause instanceof Error ? cause.message : String(cause));
      }
    },
    [ensureSession],
  );

  const loadAndPlay = useCallback(
    async (
      queue: string[],
      startIndex: number,
      nextMetadata: TtsPlayerChapter,
      settings: TtsSettings,
    ) => {
      if (queue.length === 0) {
        setError('No readable paragraphs were found in this chapter.');
        return;
      }
      setSentences(queue);
      setMetadata(nextMetadata);
      await run(async session => {
        await session.load(
          queue.map((text, index) => ({ id: String(index), text })),
          startIndex,
          {
            novelName: nextMetadata.novelName,
            chapterName: nextMetadata.chapterName,
            coverUri: nextMetadata.coverUri,
          },
          settings,
        );

        // The playback service shows a notification, which needs the
        // permission on Android 13 and newer. The request is not awaited:
        // a denied permission must not stop the narration, it only means no
        // notification is shown.
        void (async () => {
          try {
            await askForPostNotificationsPermission();
          } catch {
            // The permission is optional, so a failure is ignored.
          }
        })();

        await session.play();
      });
    },
    [run],
  );

  const command = useCallback(
    (nextCommand: TtsCommand) => {
      void run(session => {
        switch (nextCommand) {
          case 'next':
            return session.skipNext();
          case 'pause':
            return session.pause();
          case 'play':
            return session.play();
          case 'previous':
            return session.skipPrevious();
          case 'replay':
            return session.replayCurrent();
          case 'stop':
            return session.stop();
        }
      });
    },
    [run],
  );

  const seekTo = useCallback(
    (index: number) => {
      void run(session => session.seekTo(index));
    },
    [run],
  );

  const updateSettings = useCallback(
    (settings: TtsSettings) => {
      if (sessionRef.current) {
        void run(session => session.updateSettings(settings));
      }
    },
    [run],
  );

  const clearSleepTimer = useCallback(() => {
    if (sleepTimerRef.current) {
      clearInterval(sleepTimerRef.current);
      sleepTimerRef.current = null;
    }
    setSleepTimerMinutes(0);
  }, []);

  const setSleepTimer = useCallback(
    (minutes: number) => {
      clearSleepTimer();

      if (minutes <= 0) {
        if (sessionRef.current) {
          void run(session => session.setSleepTimer(0));
        }
        return;
      }

      setSleepTimerMinutes(minutes);
      if (sessionRef.current) {
        void run(session => session.setSleepTimer(minutes));
      }

      // The countdown is only for the UI. The native timer does the actual
      // stopping, so it keeps working while the app is in the background.
      sleepTimerRef.current = setInterval(() => {
        setSleepTimerMinutes(current => {
          if (current <= 1) {
            clearSleepTimer();
            return 0;
          }
          return current - 1;
        });
      }, 60_000);
    },
    [clearSleepTimer, run],
  );

  const stop = useCallback(() => {
    clearSleepTimer();
    setSentences([]);
    setMetadata(undefined);
    void run(session => session.stop());
  }, [clearSleepTimer, run]);

  useEffect(() => {
    void ensureSession().catch(cause => {
      if (mountedRef.current) {
        setError(cause instanceof Error ? cause.message : String(cause));
      }
    });
    return () => {
      mountedRef.current = false;
      subscriptionsRef.current.forEach(subscription => subscription.remove());
      subscriptionsRef.current = [];
    };
  }, [ensureSession]);

  useEffect(
    () => () => {
      if (sleepTimerRef.current) {
        clearInterval(sleepTimerRef.current);
      }
    },
    [],
  );

  const currentSentence = useMemo(() => {
    const index = progress.index;
    if (!Number.isInteger(index) || index < 0 || index >= sentences.length) {
      return '';
    }
    return sentences[index];
  }, [progress.index, sentences]);

  const value = useMemo<TtsPlayerContextValue>(
    () => ({
      state,
      progress,
      currentSentence,
      sentences,
      metadata,
      error,
      sleepTimerMinutes,
      hasQueue: sentences.length > 0,
      loadAndPlay,
      command,
      seekTo,
      updateSettings,
      setSleepTimer,
      stop,
    }),
    [
      command,
      currentSentence,
      error,
      loadAndPlay,
      metadata,
      progress,
      seekTo,
      sentences,
      setSleepTimer,
      sleepTimerMinutes,
      state,
      stop,
      updateSettings,
    ],
  );

  return (
    <TtsPlayerContext.Provider value={value}>
      {children}
    </TtsPlayerContext.Provider>
  );
};

export const useTtsPlayer = (): TtsPlayerContextValue =>
  useContext(TtsPlayerContext);

export { TtsPlayerContext };

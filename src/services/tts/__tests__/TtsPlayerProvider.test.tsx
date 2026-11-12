import { act, render, screen, waitFor } from '@testing-library/react-native';
import { useEffect, useRef } from 'react';
import { Text } from 'react-native';

import { Tts, type TtsSession } from '@modules/nitro-tts';
import {
  TtsPlayerProvider,
  useTtsPlayer,
} from '@services/tts/TtsPlayerProvider';

const getNativeSession = async (): Promise<jest.Mocked<TtsSession>> => {
  const createSession = Tts.createSession as jest.Mock;

  // The native session is created on the first command, not when the
  // provider is mounted.
  await act(async () => {
    getPlayer().command('play');
  });

  await waitFor(() => expect(createSession).toHaveBeenCalled());
  return createSession.mock.results[0].value as Promise<
    jest.Mocked<TtsSession>
  >;
};

const playerRef: { current: ReturnType<typeof useTtsPlayer> | null } = {
  current: null,
};

const Probe = () => {
  const player = useTtsPlayer();
  const ref = useRef(player);
  ref.current = player;

  useEffect(() => {
    playerRef.current = ref.current;
  });

  return (
    <Text testID="probe">{`${player.state}|${player.currentSentence}`}</Text>
  );
};

const renderProvider = () =>
  render(
    <TtsPlayerProvider>
      <Probe />
    </TtsPlayerProvider>,
  );

const getPlayer = () => {
  if (!playerRef.current) {
    throw new Error('The provider was not rendered.');
  }
  return playerRef.current;
};

describe('TtsPlayerProvider', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    playerRef.current = null;
  });

  it('loads the queue before playing it', async () => {
    renderProvider();
    const session = await getNativeSession();

    await act(async () => {
      await getPlayer().loadAndPlay(
        ['First sentence.', 'Second sentence.'],
        1,
        { novelName: 'Novel', chapterName: 'Chapter' },
        { rate: 1, pitch: 1 },
      );
    });

    expect(session.load).toHaveBeenCalledWith(
      [
        { id: '0', text: 'First sentence.' },
        { id: '1', text: 'Second sentence.' },
      ],
      1,
      expect.objectContaining({ novelName: 'Novel', chapterName: 'Chapter' }),
      { engineName: undefined, voiceIdentifier: undefined, rate: 1, pitch: 1 },
    );
    expect(session.play).toHaveBeenCalled();
  });

  it('exposes the sentence that is currently spoken', async () => {
    renderProvider();
    await getNativeSession();

    await act(async () => {
      await getPlayer().loadAndPlay(
        ['First sentence.', 'Second sentence.'],
        0,
        { novelName: 'Novel', chapterName: 'Chapter' },
        { rate: 1, pitch: 1 },
      );
    });

    expect(getPlayer().sentences).toEqual([
      'First sentence.',
      'Second sentence.',
    ]);
    expect(getPlayer().hasQueue).toBe(true);
    expect(screen.getByTestId('probe').props.children).toBe(
      'idle|First sentence.',
    );
  });

  it('forwards transport commands to the native session', async () => {
    renderProvider();
    const session = await getNativeSession();

    await act(async () => {
      getPlayer().command('play');
      await Promise.resolve();
    });

    expect(session.play).toHaveBeenCalled();
  });

  it('sets and clears the sleep timer', async () => {
    renderProvider();
    const session = await getNativeSession();

    await act(async () => {
      getPlayer().setSleepTimer(15);
      await Promise.resolve();
    });

    expect(session.setSleepTimer).toHaveBeenCalledWith(15);
    expect(getPlayer().sleepTimerMinutes).toBe(15);

    await act(async () => {
      getPlayer().setSleepTimer(0);
      await Promise.resolve();
    });

    expect(session.setSleepTimer).toHaveBeenCalledWith(0);
    expect(getPlayer().sleepTimerMinutes).toBe(0);
  });
});

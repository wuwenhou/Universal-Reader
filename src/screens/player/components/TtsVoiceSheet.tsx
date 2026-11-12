import { useCallback, useEffect, useState } from 'react';
import { Pressable, ScrollView, StyleSheet, View } from 'react-native';
import { Text } from 'react-native-paper';

import { Dialog } from '@components';
import { useTheme } from '@hooks/persisted';
import { useChapterReaderSettings } from '@hooks/persisted/useSettings';
import { Tts, TtsEngine, TtsVoice } from '@modules/nitro-tts';
import { useTtsPlayer } from '@services/tts/TtsPlayerProvider';
import { toNativeTtsSettings } from '@utils/tts';

interface TtsVoiceSheetProps {
  visible: boolean;
  onDismiss: () => void;
}

/**
 * Picks the speech engine and voice.
 *
 * The choice is a reader setting, so it is shared with the reader's own TTS
 * tab and applied to the running session immediately.
 */
export const TtsVoiceSheet = ({ visible, onDismiss }: TtsVoiceSheetProps) => {
  const theme = useTheme();
  const { tts, setChapterReaderSettings } = useChapterReaderSettings();
  const { updateSettings } = useTtsPlayer();

  const [engines, setEngines] = useState<TtsEngine[]>([]);
  const [voices, setVoices] = useState<TtsVoice[]>([]);

  useEffect(() => {
    if (!visible) {
      return;
    }

    Tts.getEngines()
      .then(res => {
        setEngines([...res].sort((a, b) => a.label.localeCompare(b.label)));
      })
      .catch(() => setEngines([]));
  }, [visible]);

  const engineName = tts?.engine?.name;
  useEffect(() => {
    if (!visible) {
      return;
    }

    Tts.getVoices(engineName)
      .then(res => {
        setVoices([...res].sort((a, b) => a.name.localeCompare(b.name)));
      })
      .catch(() => setVoices([]));
  }, [engineName, visible]);

  const apply = useCallback(
    (next: { engine?: TtsEngine; voice?: TtsVoice }) => {
      const settings = {
        ...tts,
        engine: next.engine,
        voice: next.voice,
      };
      setChapterReaderSettings({ tts: settings });
      updateSettings(toNativeTtsSettings(settings));
    },
    [setChapterReaderSettings, tts, updateSettings],
  );

  const selectEngine = useCallback(
    (engine?: TtsEngine) => {
      // Voice identifiers are engine-scoped, so switching engines clears the
      // previously selected voice rather than carrying over a stale one.
      apply({ engine, voice: undefined });
    },
    [apply],
  );

  const selectVoice = useCallback(
    (voice?: TtsVoice) => {
      apply({ engine: tts?.engine, voice });
    },
    [apply, tts?.engine],
  );

  return (
    <Dialog.Root visible={visible} onDismiss={onDismiss}>
      <Dialog.Title>Voice</Dialog.Title>
      <Dialog.ScrollArea style={styles.scrollArea}>
        <ScrollView>
          {engines.length > 0 ? (
            <View>
              <Text
                style={[styles.subheader, { color: theme.onSurfaceVariant }]}
              >
                Engine
              </Text>
              {engines.map(engine => (
                <Pressable
                  key={engine.name}
                  onPress={() => selectEngine(engine)}
                  style={styles.option}
                  accessibilityRole="button"
                >
                  <Text style={{ color: theme.onSurface }}>{engine.label}</Text>
                </Pressable>
              ))}
            </View>
          ) : null}

          <Text style={[styles.subheader, { color: theme.onSurfaceVariant }]}>
            Voice
          </Text>
          <Pressable
            onPress={() => selectVoice(undefined)}
            style={styles.option}
            accessibilityRole="button"
          >
            <Text style={{ color: theme.onSurface }}>System default</Text>
          </Pressable>
          {voices.map(voice => (
            <Pressable
              key={voice.identifier}
              onPress={() => selectVoice(voice)}
              style={styles.option}
              accessibilityRole="button"
            >
              <Text style={{ color: theme.onSurface }}>{voice.name}</Text>
            </Pressable>
          ))}
        </ScrollView>
      </Dialog.ScrollArea>
      <Dialog.Actions>
        <Dialog.Action onPress={onDismiss}>Close</Dialog.Action>
      </Dialog.Actions>
    </Dialog.Root>
  );
};

const styles = StyleSheet.create({
  scrollArea: { maxHeight: 380 },
  subheader: { fontSize: 12, paddingHorizontal: 16, paddingVertical: 8 },
  option: {
    alignItems: 'center',
    borderRadius: 8,
    paddingHorizontal: 16,
    paddingVertical: 12,
  },
});

import { useCallback } from 'react';
import { StyleSheet, View } from 'react-native';
import { Text } from 'react-native-paper';

import { Dialog, Slider } from '@components';
import { useTheme } from '@hooks/persisted';
import { useChapterReaderSettings } from '@hooks/persisted/useSettings';
import { useTtsPlayer } from '@services/tts/TtsPlayerProvider';
import { toNativeTtsSettings } from '@utils/tts';

interface TtsSpeedSheetProps {
  visible: boolean;
  onDismiss: () => void;
  steps: number[];
}

/**
 * Picks the speech speed.
 *
 * The speed is a reader setting, so it is shared with the reader's own TTS
 * tab and applied to the running session immediately.
 */
export const TtsSpeedSheet = ({
  visible,
  onDismiss,
  steps,
}: TtsSpeedSheetProps) => {
  const theme = useTheme();
  const { tts, setChapterReaderSettings } = useChapterReaderSettings();
  const { updateSettings } = useTtsPlayer();

  const rate = tts?.rate ?? 1;

  const applyRate = useCallback(
    (value: number) => {
      const next = { ...tts, rate: value };
      setChapterReaderSettings({ tts: next });
      updateSettings(toNativeTtsSettings(next));
    },
    [setChapterReaderSettings, tts, updateSettings],
  );

  return (
    <Dialog.Root visible={visible} onDismiss={onDismiss}>
      <Dialog.Title>Speed</Dialog.Title>
      <Dialog.Content>
        <View style={styles.sliderSection}>
          <Text style={[styles.label, { color: theme.onSurface }]}>
            {`${rate.toFixed(2)}x`}
          </Text>
          <Slider
            value={rate}
            min={steps[0]}
            max={steps[steps.length - 1]}
            step={steps[1] - steps[0]}
            showValueIndicator
            formatValue={value => `${value.toFixed(2)}x`}
            accessibilityLabel="Speech speed"
            onSlidingComplete={applyRate}
          />
        </View>
      </Dialog.Content>
      <Dialog.Actions>
        <Dialog.Action onPress={onDismiss}>Close</Dialog.Action>
      </Dialog.Actions>
    </Dialog.Root>
  );
};

const styles = StyleSheet.create({
  sliderSection: { gap: 8 },
  label: { fontSize: 16 },
});

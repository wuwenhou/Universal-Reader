import { useCallback } from 'react';
import { Pressable, ScrollView, StyleSheet } from 'react-native';
import { Text } from 'react-native-paper';

import { Dialog } from '@components';
import { useTheme } from '@hooks/persisted';
import { useTtsPlayer } from '@services/tts/TtsPlayerProvider';

interface TtsSleepTimerSheetProps {
  visible: boolean;
  onDismiss: () => void;
}

const TIMER_OPTIONS = [5, 10, 15, 30, 45, 60, 90];

/**
 * Picks the time after which playback stops.
 *
 * The timer itself is handled by the native session, so it keeps running
 * even when the app is in the background.
 */
export const TtsSleepTimerSheet = ({
  visible,
  onDismiss,
}: TtsSleepTimerSheetProps) => {
  const theme = useTheme();
  const { sleepTimerMinutes, setSleepTimer } = useTtsPlayer();

  const select = useCallback(
    (minutes: number) => {
      setSleepTimer(minutes);
      onDismiss();
    },
    [onDismiss, setSleepTimer],
  );

  return (
    <Dialog.Root visible={visible} onDismiss={onDismiss}>
      <Dialog.Title>Sleep timer</Dialog.Title>
      <Dialog.ScrollArea style={styles.scrollArea}>
        <ScrollView>
          {TIMER_OPTIONS.map(minutes => (
            <Pressable
              key={minutes}
              onPress={() => select(minutes)}
              style={[
                styles.option,
                sleepTimerMinutes === minutes && {
                  backgroundColor: theme.surfaceVariant,
                },
              ]}
              accessibilityRole="button"
            >
              <Text style={{ color: theme.onSurface }}>{`${minutes} min`}</Text>
            </Pressable>
          ))}
          {sleepTimerMinutes > 0 ? (
            <Pressable
              onPress={() => select(0)}
              style={styles.option}
              accessibilityRole="button"
            >
              <Text style={{ color: theme.error }}>Turn off</Text>
            </Pressable>
          ) : null}
        </ScrollView>
      </Dialog.ScrollArea>
      <Dialog.Actions>
        <Dialog.Action onPress={onDismiss}>Close</Dialog.Action>
      </Dialog.Actions>
    </Dialog.Root>
  );
};

const styles = StyleSheet.create({
  scrollArea: { maxHeight: 320 },
  option: {
    alignItems: 'center',
    borderRadius: 8,
    paddingHorizontal: 16,
    paddingVertical: 12,
  },
});

import { Pressable, StyleSheet, View } from 'react-native';
import { Text } from 'react-native-paper';

import IconButtonV2 from '../IconButtonV2/IconButtonV2';
import { useTheme } from '@hooks/persisted';
import { useTtsPlayer } from '@services/tts/TtsPlayerProvider';

interface TtsMiniPlayerProps {
  onPress: () => void;
}

/**
 * A compact playback bar shown while something is being read aloud.
 *
 * It gives access to the full listening player without leaving the screen
 * the user is on.
 */
const TtsMiniPlayer = ({ onPress }: TtsMiniPlayerProps) => {
  const theme = useTheme();
  const { state, metadata, currentSentence, command, hasQueue } =
    useTtsPlayer();

  if (!hasQueue) {
    return null;
  }

  const isPlaying = state === 'playing';

  return (
    <Pressable
      onPress={onPress}
      style={[
        styles.container,
        { backgroundColor: theme.surfaceVariant, borderColor: theme.outline },
      ]}
      accessibilityRole="button"
      accessibilityLabel={metadata?.chapterName ?? 'Player'}
    >
      <View style={styles.textContainer}>
        <Text
          numberOfLines={1}
          style={[styles.title, { color: theme.onSurface }]}
        >
          {metadata?.chapterName ?? ''}
        </Text>
        <Text
          numberOfLines={1}
          style={[styles.subtitle, { color: theme.onSurfaceVariant }]}
        >
          {currentSentence}
        </Text>
      </View>
      <IconButtonV2
        name={isPlaying ? 'pause' : 'play'}
        onPress={() => command(isPlaying ? 'pause' : 'play')}
        theme={theme}
      />
    </Pressable>
  );
};

export default TtsMiniPlayer;

const styles = StyleSheet.create({
  container: {
    alignItems: 'center',
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderTopWidth: StyleSheet.hairlineWidth,
    flexDirection: 'row',
    gap: 8,
    paddingHorizontal: 12,
    paddingVertical: 6,
  },
  textContainer: { flex: 1 },
  title: { fontSize: 13, fontWeight: '600' },
  subtitle: { fontSize: 12 },
});

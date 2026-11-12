import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  Image,
  Pressable,
  ScrollView,
  Share,
  StyleSheet,
  View,
} from 'react-native';
import { Text } from 'react-native-paper';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { IconButtonV2, Slider } from '@components';
import { bookmarkChapter } from '@database/queries/ChapterQueries';
import { getString } from '@i18n/translations';
import { useTheme } from '@hooks/persisted';
import { useTtsPlayer } from '@services/tts/TtsPlayerProvider';
import { showToast } from '@utils/showToast';
import { TtsSpeedSheet } from './components/TtsSpeedSheet';
import { TtsSleepTimerSheet } from './components/TtsSleepTimerSheet';
import { TtsVoiceSheet } from './components/TtsVoiceSheet';

interface PlayerScreenProps {
  navigation: {
    goBack: () => void;
    navigate: (screen: string, params?: object) => void;
  };
}

const SPEED_STEPS = [0.5, 0.75, 1, 1.25, 1.5, 2, 2.5, 3];

const formatTime = (seconds: number): string => {
  if (!Number.isFinite(seconds) || seconds <= 0) {
    return '0:00';
  }

  const minutes = Math.floor(seconds / 60);
  const rest = Math.floor(seconds % 60);

  return `${minutes}:${rest.toString().padStart(2, '0')}`;
};

const PlayerScreen = ({ navigation }: PlayerScreenProps) => {
  const theme = useTheme();
  const insets = useSafeAreaInsets();
  const {
    state,
    progress,
    currentSentence,
    metadata,
    sentences,
    sleepTimerMinutes,
    command,
    seekTo,
  } = useTtsPlayer();

  const [speedSheetVisible, setSpeedSheetVisible] = useState(false);
  const [sleepSheetVisible, setSleepSheetVisible] = useState(false);
  const [voiceSheetVisible, setVoiceSheetVisible] = useState(false);

  const isPlaying = state === 'playing';
  const total = progress.total || sentences.length || 0;
  const position = Math.min(progress.index + 1, total);

  // A rough estimate of how long the queue takes, based on the average
  // length of the spoken sentences. The native engine does not report a
  // duration, so this is only used for the labels.
  const estimatedTotal = useMemo(() => {
    if (!sentences.length) {
      return 0;
    }

    const averageLength =
      sentences.reduce((sum, sentence) => sum + sentence.length, 0) /
      sentences.length;

    // ~14 characters per second at 1x speed.
    return Math.round((averageLength * sentences.length) / 14);
  }, [sentences]);

  const estimatedElapsed = useMemo(() => {
    if (!estimatedTotal || !total) {
      return 0;
    }

    return Math.round((estimatedTotal * progress.index) / total);
  }, [estimatedTotal, progress.index, total]);

  const onSeek = useCallback(
    (value: number) => {
      seekTo(Math.round(value));
    },
    [seekTo],
  );

  const openToc = useCallback(() => {
    navigation.navigate('PlayerToc');
  }, [navigation]);

  const shareChapter = useCallback(() => {
    const title = metadata?.chapterName ?? '';
    const novel = metadata?.novelName ?? '';

    void Share.share({ message: novel ? `${novel} - ${title}` : title });
  }, [metadata?.chapterName, metadata?.novelName]);

  const toggleBookmark = useCallback(() => {
    if (!metadata?.chapterId) {
      return;
    }

    void bookmarkChapter(metadata.chapterId)
      .then(() => showToast(getString('playerScreen.bookmarkToggled')))
      .catch(cause =>
        showToast(cause instanceof Error ? cause.message : String(cause)),
      );
  }, [metadata]);

  useEffect(() => {
    if (state === 'completed') {
      command('stop');
    }
  }, [command, state]);

  return (
    <View
      style={[
        styles.container,
        {
          backgroundColor: theme.background,
          paddingTop: insets.top,
          paddingBottom: insets.bottom,
        },
      ]}
    >
      <View style={styles.header}>
        <IconButtonV2
          name="arrow-left"
          onPress={navigation.goBack}
          theme={theme}
        />
        <View style={styles.headerText}>
          <Text
            numberOfLines={1}
            style={[styles.headerTitle, { color: theme.onSurface }]}
          >
            {metadata?.novelName ?? getString('playerScreen.noChapter')}
          </Text>
          {metadata?.chapterName ? (
            <Text
              numberOfLines={1}
              style={[styles.headerSubtitle, { color: theme.onSurfaceVariant }]}
            >
              {metadata.chapterName}
            </Text>
          ) : null}
        </View>
        <IconButtonV2
          name="book-open-page-variant"
          onPress={openToc}
          theme={theme}
        />
      </View>

      <ScrollView
        contentContainerStyle={styles.content}
        showsVerticalScrollIndicator={false}
      >
        {metadata?.coverUri ? (
          <Image
            source={{ uri: metadata.coverUri }}
            style={[styles.cover, { backgroundColor: theme.surfaceVariant }]}
            resizeMode="cover"
          />
        ) : (
          <View
            style={[
              styles.cover,
              styles.coverPlaceholder,
              { backgroundColor: theme.surfaceVariant },
            ]}
          >
            <Text style={{ color: theme.onSurfaceVariant }}>♪</Text>
          </View>
        )}

        <View style={styles.sentenceBox}>
          <Text
            style={[styles.sentence, { color: theme.onSurface }]}
            numberOfLines={6}
          >
            {currentSentence || getString('playerScreen.noSentence')}
          </Text>
        </View>

        <View style={styles.seekBar}>
          <Slider
            value={Math.min(progress.index, Math.max(total - 1, 0))}
            min={0}
            max={Math.max(total - 1, 0)}
            step={1}
            disabled={!total}
            showValueIndicator={false}
            accessibilityLabel={getString('playerScreen.seekBar')}
            onSlidingComplete={onSeek}
          />
          <View style={styles.timeRow}>
            <Text style={[styles.time, { color: theme.onSurfaceVariant }]}>
              {formatTime(estimatedElapsed)}
            </Text>
            <Text style={[styles.time, { color: theme.onSurfaceVariant }]}>
              {`${position}/${total}`}
            </Text>
          </View>
        </View>

        <View style={styles.controls}>
          <IconButtonV2
            name="skip-previous"
            size={32}
            onPress={() => command('previous')}
            theme={theme}
          />
          <Pressable
            onPress={() => command(isPlaying ? 'pause' : 'play')}
            style={[styles.playButton, { backgroundColor: theme.primary }]}
            accessibilityRole="button"
            accessibilityLabel={
              isPlaying
                ? getString('playerScreen.pause')
                : getString('playerScreen.play')
            }
          >
            <Text
              style={[styles.playIcon, { color: theme.onPrimary }]}
              allowFontScaling={false}
            >
              {isPlaying ? '❚❚' : '▶'}
            </Text>
          </Pressable>
          <IconButtonV2
            name="skip-next"
            size={32}
            onPress={() => command('next')}
            theme={theme}
          />
        </View>

        <View style={styles.actions}>
          <Pressable
            onPress={() => setSpeedSheetVisible(true)}
            style={styles.action}
            accessibilityRole="button"
          >
            <Text style={[styles.actionLabel, { color: theme.onSurface }]}>
              {getString('playerScreen.speed')}
            </Text>
          </Pressable>
          <Pressable
            onPress={() => setSleepSheetVisible(true)}
            style={styles.action}
            accessibilityRole="button"
          >
            <Text style={[styles.actionLabel, { color: theme.onSurface }]}>
              {sleepTimerMinutes > 0
                ? getString('playerScreen.sleepTimerRunning', {
                    count: sleepTimerMinutes,
                  })
                : getString('playerScreen.sleepTimer')}
            </Text>
          </Pressable>
          <Pressable
            onPress={() => setVoiceSheetVisible(true)}
            style={styles.action}
            accessibilityRole="button"
          >
            <Text style={[styles.actionLabel, { color: theme.onSurface }]}>
              {getString('playerScreen.voice')}
            </Text>
          </Pressable>
        </View>

        <View style={styles.secondaryActions}>
          <IconButtonV2
            name="share-variant"
            onPress={shareChapter}
            theme={theme}
          />
          <IconButtonV2
            name="bookmark-outline"
            onPress={toggleBookmark}
            theme={theme}
          />
        </View>
      </ScrollView>

      <TtsSpeedSheet
        visible={speedSheetVisible}
        onDismiss={() => setSpeedSheetVisible(false)}
        steps={SPEED_STEPS}
      />
      <TtsSleepTimerSheet
        visible={sleepSheetVisible}
        onDismiss={() => setSleepSheetVisible(false)}
      />
      <TtsVoiceSheet
        visible={voiceSheetVisible}
        onDismiss={() => setVoiceSheetVisible(false)}
      />
    </View>
  );
};

export default PlayerScreen;

const styles = StyleSheet.create({
  container: { flex: 1 },
  header: {
    alignItems: 'center',
    flexDirection: 'row',
    gap: 8,
    paddingHorizontal: 8,
    paddingVertical: 4,
  },
  headerText: { flex: 1 },
  headerTitle: { fontSize: 16, fontWeight: '600' },
  headerSubtitle: { fontSize: 13 },
  content: {
    alignItems: 'center',
    gap: 20,
    paddingHorizontal: 24,
    paddingVertical: 16,
  },
  cover: {
    borderRadius: 12,
    height: 220,
    width: 160,
  },
  coverPlaceholder: { alignItems: 'center', justifyContent: 'center' },
  sentenceBox: { minHeight: 90, justifyContent: 'center' },
  sentence: { fontSize: 18, lineHeight: 26, textAlign: 'center' },
  seekBar: { width: '100%' },
  timeRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
  },
  time: { fontSize: 12 },
  controls: {
    alignItems: 'center',
    flexDirection: 'row',
    gap: 32,
    justifyContent: 'center',
  },
  playButton: {
    alignItems: 'center',
    borderRadius: 36,
    height: 72,
    justifyContent: 'center',
    width: 72,
  },
  playIcon: { fontSize: 26 },
  actions: {
    flexDirection: 'row',
    gap: 24,
    justifyContent: 'center',
  },
  secondaryActions: {
    flexDirection: 'row',
    gap: 24,
    justifyContent: 'center',
  },
  action: { paddingHorizontal: 8, paddingVertical: 4 },
  actionLabel: { fontSize: 14, fontWeight: '500' },
});

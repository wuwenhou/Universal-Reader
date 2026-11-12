import { useCallback, useEffect, useMemo, useState } from 'react';
import { Pressable, ScrollView, StyleSheet, View } from 'react-native';
import { Text } from 'react-native-paper';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { EmptyView, IconButtonV2, LoadingScreenV2 } from '@components';
import { ChapterInfo, NovelInfo } from '@database/types';
import { getNovelChapters } from '@database/queries/ChapterQueries';
import { getNovelById } from '@database/queries/NovelQueries';
import { useTheme } from '@hooks/persisted';
import { getString } from '@i18n/translations';
import { useTtsPlayer } from '@services/tts/TtsPlayerProvider';

interface PlayerTocScreenProps {
  navigation: {
    goBack: () => void;
    navigate: (screen: string, params?: object) => void;
  };
}

/**
 * The chapter list of the novel that is being listened to.
 *
 * It is shown from the listening player so the user can jump to another
 * chapter without leaving the player.
 */
const PlayerTocScreen = ({ navigation }: PlayerTocScreenProps) => {
  const theme = useTheme();
  const insets = useSafeAreaInsets();
  const { metadata } = useTtsPlayer();
  const [chapters, setChapters] = useState<ChapterInfo[] | undefined>();
  const [novel, setNovel] = useState<NovelInfo | undefined>();
  const [error, setError] = useState<string | undefined>();

  const novelId = metadata?.novelId;

  useEffect(() => {
    if (!novelId) {
      setChapters([]);
      return;
    }

    let active = true;

    // The novel is needed to open a chapter: the reader bootstraps it from
    // the path and the plugin, not from the id alone.
    const storedNovel = getNovelById(novelId);
    if (storedNovel) {
      setNovel(storedNovel);
    }

    getNovelChapters(novelId)
      .then(result => {
        if (active) {
          setChapters(result);
        }
      })
      .catch(cause => {
        if (active) {
          setError(cause instanceof Error ? cause.message : String(cause));
          setChapters([]);
        }
      });

    return () => {
      active = false;
    };
  }, [novelId]);

  const sortedChapters = useMemo(
    () => [...(chapters ?? [])].sort((a, b) => a.id - b.id),
    [chapters],
  );

  const renderChapter = useCallback(
    (chapter: ChapterInfo) => {
      const isCurrent = chapter.id === metadata?.chapterId;

      return (
        <Pressable
          key={chapter.id}
          onPress={() => {
            if (isCurrent || !novel) {
              navigation.goBack();
              return;
            }

            navigation.navigate('ReaderStack', {
              screen: 'Chapter',
              params: { novel, chapter },
            });
          }}
          style={[
            styles.chapter,
            isCurrent && { backgroundColor: theme.surfaceVariant },
          ]}
          accessibilityRole="button"
        >
          <Text
            style={[
              styles.chapterName,
              { color: isCurrent ? theme.primary : theme.onSurface },
            ]}
            numberOfLines={2}
          >
            {chapter.name}
          </Text>
        </Pressable>
      );
    },
    [metadata?.chapterId, navigation, novel, theme],
  );

  if (!novelId) {
    return (
      <View
        style={[
          styles.container,
          { backgroundColor: theme.background, paddingTop: insets.top },
        ]}
      >
        <IconButtonV2
          name="arrow-left"
          onPress={navigation.goBack}
          theme={theme}
        />
        <EmptyView
          description={getString('playerScreen.noChapter')}
          theme={theme}
        />
      </View>
    );
  }

  return (
    <View
      style={[
        styles.container,
        { backgroundColor: theme.background, paddingTop: insets.top },
      ]}
    >
      <View style={styles.header}>
        <IconButtonV2
          name="arrow-left"
          onPress={navigation.goBack}
          theme={theme}
        />
        <Text style={[styles.title, { color: theme.onSurface }]}>
          {getString('playerScreen.toc')}
        </Text>
      </View>

      {chapters === undefined ? (
        <LoadingScreenV2 theme={theme} />
      ) : error ? (
        <EmptyView description={error} theme={theme} />
      ) : (
        <ScrollView contentContainerStyle={styles.list}>
          {sortedChapters.map(renderChapter)}
        </ScrollView>
      )}
    </View>
  );
};

export default PlayerTocScreen;

const styles = StyleSheet.create({
  container: { flex: 1 },
  header: {
    alignItems: 'center',
    flexDirection: 'row',
    gap: 8,
    paddingHorizontal: 8,
    paddingVertical: 4,
  },
  title: { fontSize: 18, fontWeight: '600' },
  list: { paddingBottom: 24, paddingHorizontal: 12 },
  chapter: {
    borderRadius: 8,
    paddingHorizontal: 12,
    paddingVertical: 10,
  },
  chapterName: { fontSize: 15 },
});

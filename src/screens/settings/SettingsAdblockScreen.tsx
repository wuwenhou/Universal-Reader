import { useCallback, useEffect, useState } from 'react';
import { ScrollView, StyleSheet } from 'react-native';
import { TextInput } from 'react-native-paper';

import { Appbar, Dialog, List, SafeAreaView } from '@components';
import { useBoolean } from '@hooks';
import { useAdblockSettings } from '@hooks/persisted/useAdblockSettings';
import { useTheme } from '@hooks/persisted';
import { getString } from '@i18n/translations';
import { SettingsStackParamList } from '@navigators/types';
import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { ADBLOCK_LISTS, ADBLOCK_LIST_ORDER } from '@services/adblock/lists';
import { AdblockFilterListId } from '@services/adblock/types';
import {
  addInsecureSourceException,
  getInsecureSourceExceptions,
  removeInsecureSourceException,
} from '@services/network';

import SettingSwitch from './components/SettingSwitch';

type AdblockSettingsProps = NativeStackScreenProps<
  SettingsStackParamList,
  'AdblockSettings'
>;

const AdblockSettings = ({ navigation }: AdblockSettingsProps) => {
  const theme = useTheme();
  const {
    enabled,
    enabledLists,
    exceptions,
    blockedCount,
    setAdblockSettings,
    toggleList,
    addException,
    removeException,
  } = useAdblockSettings();

  const exceptionModal = useBoolean();
  const insecureExceptionModal = useBoolean();
  const [exceptionValue, setExceptionValue] = useState('');
  const [insecureExceptions, setInsecureExceptions] = useState<string[]>([]);

  useEffect(() => {
    setInsecureExceptions(getInsecureSourceExceptions());
  }, []);

  const submitException = useCallback(() => {
    const hostname = exceptionValue
      .trim()
      .replace(/^https?:\/\//i, '')
      .split('/')[0];

    if (hostname) {
      addException(hostname);
    }

    setExceptionValue('');
    exceptionModal.setFalse();
  }, [addException, exceptionModal, exceptionValue]);

  const submitInsecureException = useCallback(() => {
    const hostname = exceptionValue
      .trim()
      .replace(/^https?:\/\//i, '')
      .split('/')[0];

    if (hostname) {
      addInsecureSourceException(hostname);
      setInsecureExceptions(getInsecureSourceExceptions());
    }

    setExceptionValue('');
    insecureExceptionModal.setFalse();
  }, [exceptionValue, insecureExceptionModal]);

  return (
    <SafeAreaView excludeTop>
      <Appbar
        title={getString('adblockSettings')}
        handleGoBack={navigation.goBack}
        theme={theme}
      />
      <ScrollView contentContainerStyle={styles.paddingBottom}>
        <List.Section>
          <SettingSwitch
            label={getString('adblockSettingsScreen.enable')}
            description={getString('adblockSettingsScreen.enableDesc')}
            value={enabled}
            onPress={() => setAdblockSettings({ enabled: !enabled })}
            theme={theme}
          />
          <List.Item
            title={getString('adblockSettingsScreen.blockedRequests')}
            description={`${blockedCount}`}
            theme={theme}
          />
        </List.Section>

        <List.Section>
          <List.SubHeader theme={theme}>
            {getString('adblockSettingsScreen.filterLists')}
          </List.SubHeader>
          {ADBLOCK_LIST_ORDER.map((id: AdblockFilterListId) => (
            <SettingSwitch
              key={id}
              label={ADBLOCK_LISTS[id].label}
              description={ADBLOCK_LISTS[id].license}
              value={enabledLists.includes(id)}
              onPress={() => toggleList(id)}
              theme={theme}
            />
          ))}
        </List.Section>

        <List.Section>
          <List.SubHeader theme={theme}>
            {getString('adblockSettingsScreen.exceptions')}
          </List.SubHeader>
          {exceptions.length === 0 ? (
            <List.Item
              title={getString('adblockSettingsScreen.noExceptions')}
              theme={theme}
            />
          ) : (
            exceptions.map(hostname => (
              <List.Item
                key={hostname}
                title={hostname}
                onPress={() => removeException(hostname)}
                theme={theme}
              />
            ))
          )}
          <List.Item
            title={getString('adblockSettingsScreen.addException')}
            icon="plus"
            onPress={exceptionModal.setTrue}
            theme={theme}
          />
        </List.Section>

        <List.Section>
          <List.SubHeader theme={theme}>
            {getString('adblockSettingsScreen.insecureSources')}
          </List.SubHeader>
          {insecureExceptions.length === 0 ? (
            <List.Item
              title={getString('adblockSettingsScreen.noInsecureSources')}
              theme={theme}
            />
          ) : (
            insecureExceptions.map(hostname => (
              <List.Item
                key={hostname}
                title={hostname}
                onPress={() => {
                  removeInsecureSourceException(hostname);
                  setInsecureExceptions(getInsecureSourceExceptions());
                }}
                theme={theme}
              />
            ))
          )}
          <List.Item
            title={getString('adblockSettingsScreen.addInsecureSource')}
            icon="plus"
            onPress={insecureExceptionModal.setTrue}
            theme={theme}
          />
        </List.Section>
      </ScrollView>

      <Dialog.Root
        visible={exceptionModal.value}
        onDismiss={exceptionModal.setFalse}
      >
        <Dialog.Title>
          {getString('adblockSettingsScreen.addException')}
        </Dialog.Title>
        <Dialog.Content>
          <TextInput
            autoFocus
            value={exceptionValue}
            onChangeText={setExceptionValue}
            placeholder="example.com"
            mode="outlined"
            underlineColor={theme.outline}
            theme={{ colors: { ...theme } }}
          />
        </Dialog.Content>
        <Dialog.Actions>
          <Dialog.Action onPress={exceptionModal.setFalse}>
            {getString('common.cancel')}
          </Dialog.Action>
          <Dialog.Action onPress={submitException}>
            {getString('common.add')}
          </Dialog.Action>
        </Dialog.Actions>
      </Dialog.Root>
      <Dialog.Root
        visible={insecureExceptionModal.value}
        onDismiss={insecureExceptionModal.setFalse}
      >
        <Dialog.Title>
          {getString('adblockSettingsScreen.addInsecureSource')}
        </Dialog.Title>
        <Dialog.Content>
          <TextInput
            autoFocus
            value={exceptionValue}
            onChangeText={setExceptionValue}
            placeholder="example.com"
            mode="outlined"
            underlineColor={theme.outline}
            theme={{ colors: { ...theme } }}
          />
        </Dialog.Content>
        <Dialog.Actions>
          <Dialog.Action onPress={insecureExceptionModal.setFalse}>
            {getString('common.cancel')}
          </Dialog.Action>
          <Dialog.Action onPress={submitInsecureException}>
            {getString('common.add')}
          </Dialog.Action>
        </Dialog.Actions>
      </Dialog.Root>
    </SafeAreaView>
  );
};

export default AdblockSettings;

const styles = StyleSheet.create({
  paddingBottom: { paddingBottom: 24 },
});

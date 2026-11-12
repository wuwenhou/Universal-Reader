import { ChapterReaderSettings } from '@hooks/persisted/useSettings';
import { TtsSettings } from '@modules/nitro-tts';

/**
 * Converts the reader's TTS preferences into the shape the native session
 * expects.
 */
export const toNativeTtsSettings = (
  settings: ChapterReaderSettings['tts'],
): TtsSettings => ({
  engineName: settings?.engine?.name,
  voiceIdentifier: settings?.voice?.identifier,
  rate: settings?.rate ?? 1,
  pitch: settings?.pitch ?? 1,
});

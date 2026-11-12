/**
 * Bundled uBO-compatible filter lists.
 *
 * The lists are inlined at build time (see the `inline-import` babel plugin),
 * so the app can block ads without downloading anything at runtime.
 *
 * - EasyList: the main advertising filter list (GPL-3.0 / CC BY-SA 3.0).
 * - EasyPrivacy: tracker blocking list (GPL-3.0 / CC BY-SA 3.0).
 * - ABPVN: Vietnamese ad blocking list (GPL-3.0).
 */
import easyListText from '../../../assets/adblock/easylist.txt';
import easyPrivacyText from '../../../assets/adblock/easyprivacy.txt';
import abpvnText from '../../../assets/adblock/abpvn.txt';

import { AdblockFilterListId, AdblockListDescriptor } from './types';

export const ADBLOCK_LISTS: Record<
  AdblockFilterListId,
  AdblockListDescriptor & { text: string }
> = {
  easylist: {
    id: 'easylist',
    label: 'EasyList',
    url: 'https://easylist.to/easylist/easylist.txt',
    license: 'GPL-3.0 / CC BY-SA 3.0',
    text: easyListText,
  },
  easyprivacy: {
    id: 'easyprivacy',
    label: 'EasyPrivacy',
    url: 'https://easylist.to/easylist/easyprivacy.txt',
    license: 'GPL-3.0 / CC BY-SA 3.0',
    text: easyPrivacyText,
  },
  abpvn: {
    id: 'abpvn',
    label: 'ABPVN',
    url: 'https://abpvn.com/filter/abpvn_ublock.txt',
    license: 'GPL-3.0',
    text: abpvnText,
  },
};

export const ADBLOCK_LIST_ORDER: AdblockFilterListId[] = [
  'easylist',
  'easyprivacy',
  'abpvn',
];

/**
 * Builds the filter text for the given lists.
 *
 * uBO syntax is line based, so the lists are simply concatenated. Duplicate
 * rules across lists are removed to keep the parsed engine small.
 */
export const buildFilterText = (listIds: AdblockFilterListId[]): string => {
  const seen = new Set<string>();
  const rules: string[] = [];

  for (const id of listIds) {
    const list = ADBLOCK_LISTS[id];
    if (!list) {
      continue;
    }

    for (const line of list.text.split('\n')) {
      const rule = line.trim();
      if (!rule || rule.startsWith('!') || rule.startsWith('[')) {
        continue;
      }
      if (seen.has(rule)) {
        continue;
      }
      seen.add(rule);
      rules.push(rule);
    }
  }

  return rules.join('\n');
};

import { adblockEngine } from '../engine';

describe('adblockEngine', () => {
  beforeAll(async () => {
    await adblockEngine.configure(['abpvn', 'easylist']);
  }, 60000);

  it('blocks known advertising requests', () => {
    const result = adblockEngine.match(
      'https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js',
      { type: 'script' },
    );

    expect(result.blocked).toBe(true);
  });

  it('keeps ordinary chapter requests', () => {
    const result = adblockEngine.match(
      'https://truyenfull.vn/truyen/abc/chuong-1/',
      { type: 'main_frame' },
    );

    expect(result.blocked).toBe(false);
  });

  it('never blocks the allowed host', () => {
    const result = adblockEngine.match(
      'https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js',
      { type: 'script', allowHostname: 'googlesyndication.com' },
    );

    expect(result.blocked).toBe(false);
  });

  it('returns cosmetic rules for a page', () => {
    const css = adblockEngine.getCosmeticCss('https://truyenfull.vn/');

    expect(typeof css).toBe('string');
  });
});

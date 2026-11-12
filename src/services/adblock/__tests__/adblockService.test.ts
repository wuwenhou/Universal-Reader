import { adblockEngine } from '../engine';
import { adblockService } from '../adblockService';

describe('adblockService', () => {
  beforeAll(async () => {
    await adblockEngine.configure(['abpvn', 'easylist']);
    adblockService.applySettings({
      enabled: true,
      enabledLists: ['abpvn', 'easylist'],
      exceptions: [],
    });
    await adblockService.ensureReady();
  }, 60000);

  afterEach(() => {
    adblockService.resetBlockedCount();
  });

  it('blocks a known advertising request', () => {
    expect(
      adblockService.isBlocked(
        'https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js',
        { type: 'script' },
      ),
    ).toBe(true);
  });

  it('keeps an ordinary request', () => {
    expect(
      adblockService.isBlocked('https://truyenfull.vn/truyen/abc/chuong-1/', {
        type: 'other',
      }),
    ).toBe(false);
  });

  it('counts what it blocked', () => {
    adblockService.isBlocked(
      'https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js',
      { type: 'script' },
    );

    expect(adblockService.getBlockedCount()).toBeGreaterThan(0);
  });

  it("never blocks the app's own hosts", () => {
    expect(
      adblockService.isBlocked('https://api.github.com/repos/a/b/releases', {
        type: 'other',
      }),
    ).toBe(false);
  });

  it('removes the ad resources from a chapter', () => {
    const result = adblockService.filterChapter(
      '<p>Chapter</p><img src="https://pagead2.googlesyndication.com/ads.gif" />',
      'https://truyenfull.vn/',
    );

    expect(result.html).toContain('Chapter');
    expect(result.html).not.toContain('googlesyndication');
  });
});

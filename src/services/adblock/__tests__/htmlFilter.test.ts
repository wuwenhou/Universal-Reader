import { adblockEngine } from '../engine';
import { filterChapterHtml } from '../htmlFilter';

describe('filterChapterHtml', () => {
  beforeAll(async () => {
    await adblockEngine.configure(['abpvn', 'easylist']);
  }, 60000);

  it('keeps the chapter text', () => {
    const html = '<p>Chapter content</p>';
    const result = filterChapterHtml(html, 'https://truyenfull.vn/');

    expect(result.html).toContain('Chapter content');
    expect(result.removed).toBe(0);
  });

  it('removes images that point at a known ad host', () => {
    const html = `
      <p>Chapter content</p>
      <img src="https://pagead2.googlesyndication.com/pagead/ads.gif" />
    `;

    const result = filterChapterHtml(html, 'https://truyenfull.vn/');

    expect(result.html).toContain('Chapter content');
    expect(result.html).not.toContain('googlesyndication');
    expect(result.removed).toBeGreaterThan(0);
  });

  it('keeps images that belong to the source site', () => {
    const html = '<img src="https://truyenfull.vn/cover/chapter-1.jpg" />';

    const result = filterChapterHtml(html, 'https://truyenfull.vn/');

    expect(result.html).toContain('chapter-1.jpg');
    expect(result.removed).toBe(0);
  });

  it('keeps everything for an allowed host', () => {
    const html =
      '<img src="https://pagead2.googlesyndication.com/pagead/ads.gif" />';

    const result = filterChapterHtml(
      html,
      'https://truyenfull.vn/',
      'googlesyndication.com',
    );

    expect(result.html).toContain('googlesyndication');
    expect(result.removed).toBe(0);
  });
});

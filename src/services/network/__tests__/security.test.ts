import { isHostAllowedInsecure, isInsecureUrl } from '../security';

describe('network security', () => {
  describe('isInsecureUrl', () => {
    it('detects plain HTTP', () => {
      expect(isInsecureUrl('http://example.com/chapter')).toBe(true);
    });

    it('accepts HTTPS', () => {
      expect(isInsecureUrl('https://example.com/chapter')).toBe(false);
    });

    it('accepts a URL it cannot parse', () => {
      expect(isInsecureUrl('not a url')).toBe(false);
    });
  });

  describe('isHostAllowedInsecure', () => {
    it('allows the exact host', () => {
      expect(isHostAllowedInsecure('example.com', ['example.com'])).toBe(true);
    });

    it('allows a subdomain of an allowed host', () => {
      expect(isHostAllowedInsecure('cdn.example.com', ['example.com'])).toBe(
        true,
      );
    });

    it('allows a wildcard entry', () => {
      expect(isHostAllowedInsecure('example.com', ['*.example.com'])).toBe(
        true,
      );
    });

    it('does not allow a different host', () => {
      expect(isHostAllowedInsecure('example.org', ['example.com'])).toBe(false);
    });

    it('does not allow a host that only ends with the allowed name', () => {
      expect(isHostAllowedInsecure('notexample.com', ['example.com'])).toBe(
        false,
      );
    });
  });
});

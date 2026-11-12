import { Plugin } from './types';

/**
 * The registry of loaded plugins.
 *
 * It is kept separate from the plugin manager so that reading a plugin does
 * not pull in the manager's heavier dependencies (plugin decryption, HTML
 * parsing). The reader only needs the site of a plugin.
 */
const plugins: Record<string, Plugin | undefined> = {};

export const setPlugin = (pluginId: string, plugin: Plugin | undefined) => {
  plugins[pluginId] = plugin;
};

export const getPlugin = (pluginId: string) => plugins[pluginId];

export const getPlugins = () => plugins;

/**
 * Hostnames of every loaded plugin's site.
 *
 * The ad blocker uses this to never block the source site itself - the
 * chapter text and the images that belong to it are needed to read.
 */
export const getPluginHostnames = (): string[] => {
  const hostnames = new Set<string>();

  Object.values(plugins).forEach(plugin => {
    if (!plugin?.site) {
      return;
    }
    try {
      hostnames.add(new URL(plugin.site).hostname.toLowerCase());
    } catch {
      // A plugin without a parsable site cannot be blocked by hostname.
    }
  });

  return Array.from(hostnames);
};

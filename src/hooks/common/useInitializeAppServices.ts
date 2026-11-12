import { useEffect, useState } from 'react';

import { initializeInstalledPlugins } from '@plugins/pluginManager';
import { backgroundTasks } from '@services/backgroundTasks';
import { installAdblockFetchInterceptor } from '@services/adblock/fetchInterceptor';

type AppServicesState = {
  ready: boolean;
  error?: Error;
};

let initializationPromise: Promise<void> | undefined;

const initializeAppServices = (): Promise<void> => {
  if (!initializationPromise) {
    initializationPromise = initializeInstalledPlugins()
      .then(async () => {
        await backgroundTasks.refresh();
      })
      .catch(error => {
        initializationPromise = undefined;
        throw error;
      });
  }

  return initializationPromise;
};

export const useInitializeAppServices = (
  databaseReady: boolean,
): AppServicesState => {
  const [state, setState] = useState<AppServicesState>({ ready: false });

  useEffect(() => {
    if (!databaseReady) return;

    let isActive = true;

    // The interceptor is installed before anything can make a request, so no
    // ad request can slip through before the engine is ready.
    installAdblockFetchInterceptor();

    initializeAppServices()
      .then(() => {
        if (isActive) setState({ ready: true });
      })
      .catch(error => {
        if (!isActive) return;

        setState({
          ready: false,
          error: error instanceof Error ? error : new Error(String(error)),
        });
      });

    return () => {
      isActive = false;
    };
  }, [databaseReady]);

  return state;
};

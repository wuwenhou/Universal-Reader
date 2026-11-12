import React from 'react';
import { StyleSheet, View } from 'react-native';

import { ErrorScreenV2 } from '@components';
import { getString } from '@i18n/translations';

interface ReaderErrorBoundaryProps {
  children: React.ReactNode;
  /** Called when the reader wants to try the chapter again. */
  onRetry?: () => void;
}

/**
 * Keeps a failure inside the reader from taking the whole app down.
 *
 * A chapter comes from the internet, so it can contain markup the WebView
 * chokes on. Without a boundary such a chapter would close the app instead
 * of showing an error the user can act on.
 */
class ReaderErrorBoundary extends React.Component<
  ReaderErrorBoundaryProps,
  { error: Error | null }
> {
  constructor(props: ReaderErrorBoundaryProps) {
    super(props);
    this.state = { error: null };
  }

  static getDerivedStateFromError(error: Error) {
    return { error };
  }

  componentDidUpdate(prevProps: ReaderErrorBoundaryProps) {
    // A new chapter gets a fresh chance: the previous failure must not
    // keep the reader stuck on the error screen.
    if (prevProps.children !== this.props.children && this.state.error) {
      this.setState({ error: null });
    }
  }

  render() {
    const { error } = this.state;
    const { children, onRetry } = this.props;

    if (!error) {
      return children;
    }

    return (
      <View style={styles.container}>
        <ErrorScreenV2
          error={error}
          actions={
            onRetry
              ? [
                  {
                    iconName: 'refresh',
                    title: getString('common.retry'),
                    onPress: onRetry,
                  },
                ]
              : undefined
          }
        />
      </View>
    );
  }
}

export default ReaderErrorBoundary;

const styles = StyleSheet.create({
  container: { flex: 1 },
});

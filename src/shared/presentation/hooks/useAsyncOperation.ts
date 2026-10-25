import {useCallback, useState} from 'react';

export function useAsyncOperation(onError?: (error: unknown) => void) {
  const [loading, setLoading] = useState(false);
  const execute = useCallback(async <T,>(operation: () => Promise<T>): Promise<T | undefined> => {
    if (loading) return undefined;
    setLoading(true);
    try { return await operation(); } catch (error) { onError?.(error); return undefined; } finally { setLoading(false); }
  }, [loading, onError]);
  return {loading, execute};
}

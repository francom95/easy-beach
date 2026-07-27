import {useEffect, useState} from 'react';
import NetInfo from '@react-native-community/netinfo';

/** `conectado` arranca en `true` (optimista) para no mostrar el banner offline en el primer render antes de que NetInfo responda. */
export function useNetworkStatus(): boolean {
  const [conectado, setConectado] = useState(true);

  useEffect(() => {
    const unsubscribe = NetInfo.addEventListener(estado => {
      setConectado(Boolean(estado.isConnected && estado.isInternetReachable !== false));
    });
    return unsubscribe;
  }, []);

  return conectado;
}

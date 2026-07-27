import * as Keychain from 'react-native-keychain';

/**
 * Refresh token guardado en el Keystore/Keychain nativo (etapa 16: "sesiones
 * largas de temporada" - etapa 05 dice 60 días de TTL, rotativo). El access
 * token NO se persiste acá: vive solo en memoria (ver authStore) porque su
 * TTL es corto (60 min) y no vale la pena el I/O nativo por algo efímero.
 */
const SERVICE = 'com.easybeach.mobile.refreshToken';

export async function guardarRefreshToken(refreshToken: string): Promise<void> {
  await Keychain.setGenericPassword('easybeach', refreshToken, {service: SERVICE});
}

export async function leerRefreshToken(): Promise<string | null> {
  const resultado = await Keychain.getGenericPassword({service: SERVICE});
  return resultado ? resultado.password : null;
}

export async function borrarRefreshToken(): Promise<void> {
  await Keychain.resetGenericPassword({service: SERVICE});
}

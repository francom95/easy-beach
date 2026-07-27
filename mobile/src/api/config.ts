import {Platform} from 'react-native';

/**
 * `10.0.2.2` es el loopback especial del emulador de Android hacia el
 * `localhost` de la máquina host (AVD, no un dispositivo físico); iOS
 * Simulator sí resuelve `localhost` directo. En un dispositivo físico esto
 * tiene que apuntar a la IP real del backend - no hay forma de saberlo en
 * build-time, así que queda como constante a mano hasta que exista un
 * entorno desplegado real (etapa 20).
 */
const HOST = Platform.OS === 'android' ? '10.0.2.2' : 'localhost';

export const API_BASE_URL = `http://${HOST}:8080/api/v1`;

/** Debe superar el timeout de la etapa 04 §7 para pedidos con conexión de playa mala. */
export const REQUEST_TIMEOUT_MS = 20000;

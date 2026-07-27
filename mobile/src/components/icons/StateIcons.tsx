import React from 'react';
import Svg, {Circle, Path} from 'react-native-svg';

type IconProps = {color: string; size?: number};

/**
 * Iconos mínimos vía react-native-svg (JS puro + módulo nativo autolinkeado
 * por RN 0.86, sin necesidad de empaquetar fuentes de íconos como
 * react-native-vector-icons exige - menos riesgo de configuración nativa
 * para un set tan chico). Regla dura de accesibilidad (etapa 06): los
 * estados SIEMPRE van icono + texto, nunca color solo.
 */

export function IconCheckCircle({color, size = 24}: IconProps) {
  return (
    <Svg width={size} height={size} viewBox="0 0 24 24" fill="none">
      <Circle cx={12} cy={12} r={10} fill={color} />
      <Path d="M7.5 12.5l3 3 6-6.5" stroke="#FFFFFF" strokeWidth={2} strokeLinecap="round" strokeLinejoin="round" />
    </Svg>
  );
}

export function IconClock({color, size = 24}: IconProps) {
  return (
    <Svg width={size} height={size} viewBox="0 0 24 24" fill="none">
      <Circle cx={12} cy={12} r={10} stroke={color} strokeWidth={2} />
      <Path d="M12 7v5l3.5 2" stroke={color} strokeWidth={2} strokeLinecap="round" strokeLinejoin="round" />
    </Svg>
  );
}

export function IconAlertTriangle({color, size = 24}: IconProps) {
  return (
    <Svg width={size} height={size} viewBox="0 0 24 24" fill="none">
      <Path
        d="M12 3.5 22 20.5H2L12 3.5Z"
        fill={color}
        stroke={color}
        strokeWidth={1.5}
        strokeLinejoin="round"
      />
      <Path d="M12 10v4" stroke="#FFFFFF" strokeWidth={2} strokeLinecap="round" />
      <Circle cx={12} cy={17} r={1.1} fill="#FFFFFF" />
    </Svg>
  );
}

export function IconXCircle({color, size = 24}: IconProps) {
  return (
    <Svg width={size} height={size} viewBox="0 0 24 24" fill="none">
      <Circle cx={12} cy={12} r={10} fill={color} />
      <Path d="M9 9l6 6M15 9l-6 6" stroke="#FFFFFF" strokeWidth={2} strokeLinecap="round" />
    </Svg>
  );
}

export function IconInfoCircle({color, size = 24}: IconProps) {
  return (
    <Svg width={size} height={size} viewBox="0 0 24 24" fill="none">
      <Circle cx={12} cy={12} r={10} fill={color} />
      <Circle cx={12} cy={7.5} r={1.2} fill="#FFFFFF" />
      <Path d="M12 11v6" stroke="#FFFFFF" strokeWidth={2} strokeLinecap="round" />
    </Svg>
  );
}

export function IconWifiOff({color, size = 24}: IconProps) {
  return (
    <Svg width={size} height={size} viewBox="0 0 24 24" fill="none">
      <Path
        d="M3 3l18 18M5 9a15 15 0 0 1 5.5-3M8.5 12.5a9 9 0 0 1 4.5-2M12 17.5v.01M15.5 15a5 5 0 0 1 1.7 1"
        stroke={color}
        strokeWidth={2}
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </Svg>
  );
}

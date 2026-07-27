import React, {useState} from 'react';
import {Text, TextInput, View} from 'react-native';
import {ScreenContainer} from '../../components/ScreenContainer';
import {Button} from '../../components/Button';
import {useTheme} from '../../theme/ThemeProvider';
import {usePaymentStore} from '../../store/paymentStore';
import type {RootScreenProps} from '../../navigation/types';

/**
 * Hoja nativa in-app (etapa 07 S16): sin redirect a navegador, sin marca de
 * terceros visible.
 *
 * <b>Alcance real de esta etapa (deferred explícito, ver entregable):</b> el
 * backend (etapa 13) ya espera un `cardToken` opaco generado por el SDK
 * de Mercado Pago en el dispositivo - la app NUNCA ve un PAN/CVV real. Acá
 * NO se integra el SDK nativo real de MP (exige credenciales de una cuenta
 * de MP real que no existen en este entorno de desarrollo, y vincular un
 * módulo nativo de terceros sin poder probarlo contra sandbox es más riesgo
 * que valor). Este formulario tiene la FORMA del checkout real pero al
 * confirmar genera un token local de prueba - ningún dato de tarjeta sale
 * nunca de este dispositivo. Mismo principio que `FakeMercadoPagoPaymentClient`
 * del lado del backend: todo lo de negocio (idempotencia, revalidación,
 * webhook, reintento) es real y se prueba real; el proveedor externo, no.
 */
export function S16HojaMedioPago({navigation}: RootScreenProps<'S16HojaMedioPago'>) {
  const {tokens, spacing, typeScale, radii} = useTheme();
  const guardarTarjeta = usePaymentStore(s => s.guardarTarjeta);
  const [numero, setNumero] = useState('');
  const [vencimiento, setVencimiento] = useState('');
  const [cvv, setCvv] = useState('');

  const estilosInput = {
    borderWidth: 1.5,
    borderColor: tokens['color.border'],
    borderRadius: radii.control,
    padding: spacing.md,
    color: tokens['color.on-surface'],
    marginBottom: spacing.md,
  };

  function confirmar() {
    const ultimosCuatro = numero.replace(/\s/g, '').slice(-4) || '0000';
    const tokenLocal = `local-token-${Date.now()}`;
    guardarTarjeta(tokenLocal, ultimosCuatro);
    navigation.goBack();
  }

  return (
    <ScreenContainer>
      <Text style={{fontSize: typeScale.title, fontWeight: '800', color: tokens['color.on-background'], marginBottom: spacing.lg}}>
        Agregar tarjeta
      </Text>
      <TextInput
        placeholder="Número de tarjeta"
        keyboardType="number-pad"
        value={numero}
        onChangeText={setNumero}
        style={estilosInput}
        placeholderTextColor={tokens['color.on-surface-muted']}
      />
      <View style={{flexDirection: 'row', gap: spacing.md}}>
        <TextInput
          placeholder="MM/AA"
          value={vencimiento}
          onChangeText={setVencimiento}
          style={[estilosInput, {flex: 1}]}
          placeholderTextColor={tokens['color.on-surface-muted']}
        />
        <TextInput
          placeholder="CVV"
          keyboardType="number-pad"
          secureTextEntry
          value={cvv}
          onChangeText={setCvv}
          style={[estilosInput, {flex: 1}]}
          placeholderTextColor={tokens['color.on-surface-muted']}
        />
      </View>
      <Button titulo="Guardar tarjeta" onPress={confirmar} deshabilitado={numero.length < 4} />
    </ScreenContainer>
  );
}

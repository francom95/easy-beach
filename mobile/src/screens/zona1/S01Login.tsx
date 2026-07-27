import React, {useState} from 'react';
import {Text, TextInput, View} from 'react-native';
import {ScreenContainer} from '../../components/ScreenContainer';
import {Button} from '../../components/Button';
import {EstadoError} from '../../components/EstadoCarga';
import {useTheme} from '../../theme/ThemeProvider';
import {useAuthStore} from '../../store/authStore';
import {ApiError} from '../../api/ApiError';
import type {RootScreenProps} from '../../navigation/types';

/**
 * Email + contraseña (decisión confirmada para esta etapa: el mockup de la
 * etapa 07 proponía teléfono+SMS, pero eso exige backend nuevo -
 * proveedor de SMS, verificación de teléfono - fuera del alcance mobile-only
 * de esta etapa; se adapta a lo que el backend de la etapa 09 ya construyó).
 */
export function S01Login({navigation}: RootScreenProps<'S01Login'>) {
  const {tokens, spacing, typeScale, radii} = useTheme();
  const login = useAuthStore(s => s.login);
  const registrar = useAuthStore(s => s.registrar);

  const [modoRegistro, setModoRegistro] = useState(false);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [nombre, setNombre] = useState('');
  const [cargando, setCargando] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function enviar() {
    setError(null);
    setCargando(true);
    try {
      if (modoRegistro) {
        await registrar(email.trim(), password, nombre.trim());
      } else {
        await login(email.trim(), password);
      }
      navigation.replace('S02SelectorBalneario');
    } catch (e) {
      const apiError = e as ApiError;
      setError(apiError.detail ?? 'No pudimos iniciar sesión. Probá de nuevo.');
    } finally {
      setCargando(false);
    }
  }

  const estilosInput = {
    borderWidth: 1.5,
    borderColor: tokens['color.border'],
    borderRadius: radii.control,
    padding: spacing.md,
    fontSize: typeScale.body,
    color: tokens['color.on-surface'],
    marginBottom: spacing.md,
  };

  return (
    <ScreenContainer scroll>
      <View style={{marginTop: spacing.xxl, marginBottom: spacing.xl}}>
        <Text style={{fontSize: typeScale.display, fontWeight: '800', color: tokens['color.on-background']}}>
          EasyBeach
        </Text>
        <Text style={{fontSize: typeScale.body, color: tokens['color.on-surface-muted'], marginTop: spacing.xs}}>
          {modoRegistro ? 'Creá tu cuenta para empezar' : 'Ingresá para elegir tu balneario'}
        </Text>
      </View>

      {modoRegistro ? (
        <TextInput
          placeholder="Nombre"
          value={nombre}
          onChangeText={setNombre}
          style={estilosInput}
          placeholderTextColor={tokens['color.on-surface-muted']}
        />
      ) : null}
      <TextInput
        placeholder="Email"
        value={email}
        onChangeText={setEmail}
        autoCapitalize="none"
        keyboardType="email-address"
        style={estilosInput}
        placeholderTextColor={tokens['color.on-surface-muted']}
      />
      <TextInput
        placeholder="Contraseña"
        value={password}
        onChangeText={setPassword}
        secureTextEntry
        style={estilosInput}
        placeholderTextColor={tokens['color.on-surface-muted']}
      />

      {error ? (
        <View style={{marginBottom: spacing.md}}>
          <EstadoError mensaje={error} />
        </View>
      ) : null}

      <Button
        titulo={modoRegistro ? 'Crear cuenta' : 'Ingresar'}
        onPress={enviar}
        cargando={cargando}
        deshabilitado={!email || !password || (modoRegistro && !nombre)}
      />
      <View style={{marginTop: spacing.lg, alignItems: 'center'}}>
        <Button
          titulo={modoRegistro ? 'Ya tengo cuenta' : 'Crear una cuenta nueva'}
          onPress={() => setModoRegistro(!modoRegistro)}
          variante="contorno"
          fullWidth={false}
        />
      </View>
    </ScreenContainer>
  );
}

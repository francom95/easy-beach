package com.easybeach.identity.domain;

/** Roles cerrados (etapa 05 §2). El admin de balneario hereda las
 * capacidades operativas de carpero y operador dentro de su balneario. */
public enum RolCodigo {
    CLIENTE,
    CARPERO,
    OPERADOR,
    ADMIN_BALNEARIO,
    SUPER_ADMIN
}

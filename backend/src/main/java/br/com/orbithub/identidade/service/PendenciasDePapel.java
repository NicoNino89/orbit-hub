package br.com.orbithub.identidade.service;

import br.com.orbithub.identidade.domain.Papel;
import java.util.Optional;
import java.util.UUID;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 01:40
 * Cartão Jira: KAN-16
 * Título: Pendências que impedem desativar um papel
 * Objetivo: Permitir que cada módulo diga se a conta tem pendências num papel (conexões em andamento, rodadas
 *           abertas, startups publicadas).
 * Decisões: Padrão Strategy: o serviço de perfil consulta todas as implementações registradas e não conhece
 *           os módulos. Conexões (S7), rodadas (S4/S9) e startups (S4/S5) acrescentam as suas quando
 *           existirem; até lá, não há pendências.
 */
public interface PendenciasDePapel {

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:40
     * Cartão Jira: KAN-16
     * Título: Pendência no papel
     * Objetivo: Devolver a descrição da pendência que impede desativar o papel, se houver.
     * Decisões: A descrição aparece para o usuário (por exemplo, "2 conexões em andamento").
     */
    Optional<String> pendencia(UUID usuarioId, Papel papel);
}

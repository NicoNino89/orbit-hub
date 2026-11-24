package br.com.orbithub.identidade.api;

import br.com.orbithub.identidade.domain.Papel;
import br.com.orbithub.identidade.service.SessaoService.SessaoEmitida;
import java.time.Instant;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 00:20
 * Cartão Jira: KAN-57
 * Título: Resposta de sessão
 * Objetivo: Devolver ao front-end o access token, a expiração e um resumo da conta.
 * Decisões: O refresh token nunca vai no corpo, só no cookie HttpOnly.
 */
public record SessaoResponse(String tokenAcesso, Instant expiraEm, UsuarioResumo usuario) {

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Conversão da sessão emitida
     * Objetivo: Montar a resposta a partir da sessão emitida pelo serviço.
     * Decisões: Papéis ordenados, para a resposta ser estável.
     */
    public static SessaoResponse de(SessaoEmitida sessao) {
        var usuario = sessao.usuario();
        return new SessaoResponse(sessao.tokenAcesso(), sessao.acessoExpiraEm(),
                new UsuarioResumo(usuario.getId(), usuario.getNome(), usuario.getEmail(),
                        new TreeSet<>(usuario.getPapeis())));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Resumo da conta
     * Objetivo: Dados mínimos para o front-end montar o cabeçalho e as permissões da tela.
     * Decisões: As regras de acesso continuam no servidor; os papéis aqui só orientam a interface.
     */
    public record UsuarioResumo(UUID id, String nome, String email, Set<Papel> papeis) {
    }
}

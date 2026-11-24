package br.com.orbithub.compartilhado.seguranca;

import br.com.orbithub.identidade.domain.Papel;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 00:40
 * Cartão Jira: KAN-92
 * Título: Dados do usuário autenticado
 * Objetivo: Ler do access token o id da conta, os papéis e a sessão.
 * Decisões: Funções puras sobre o Jwt que o Spring injeta com {@code @AuthenticationPrincipal}; nenhum
 *           controller lê claims "na mão".
 */
public final class Autenticado {

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:40
     * Cartão Jira: KAN-92
     * Título: Classe utilitária
     * Objetivo: Impedir instâncias.
     * Decisões: Só métodos estáticos.
     */
    private Autenticado() {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:40
     * Cartão Jira: KAN-92
     * Título: Id da conta
     * Objetivo: Devolver o id da conta dono do token.
     * Decisões: O "sub" do token é sempre o UUID da conta.
     */
    public static UUID id(Jwt token) {
        return UUID.fromString(token.getSubject());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:40
     * Cartão Jira: KAN-92
     * Título: Papel presente no token
     * Objetivo: Dizer se o token traz o papel informado.
     * Decisões: Compara pelo nome do papel (INVESTIDOR, EMPREENDEDOR, ADMIN).
     */
    public static boolean temPapel(Jwt token, String papel) {
        List<String> papeis = token.getClaimAsStringList("papeis");
        return papeis != null && papeis.contains(papel);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:40
     * Cartão Jira: KAN-92
     * Título: Sessão do token
     * Objetivo: Devolver o id da sessão (família de refresh tokens).
     * Decisões: Claim "sid", sempre presente nos tokens emitidos pela API.
     */
    public static UUID sessao(Jwt token) {
        return UUID.fromString(token.getClaimAsString("sid"));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:52
     * Cartão Jira: KAN-81
     * Título: Papéis do token
     * Objetivo: Devolver os papéis que o token traz, já como enum.
     * Decisões: Nome desconhecido é ignorado (token antigo de um papel que deixou de existir).
     */
    public static Set<Papel> papeis(Jwt token) {
        List<String> nomes = token.getClaimAsStringList("papeis");
        Set<Papel> papeis = EnumSet.noneOf(Papel.class);
        if (nomes != null) {
            for (String nome : nomes) {
                Arrays.stream(Papel.values()).filter(p -> p.name().equals(nome)).findFirst().ifPresent(papeis::add);
            }
        }
        return papeis;
    }
}

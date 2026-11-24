package br.com.orbithub.compartilhado.seguranca;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 01:50
 * Cartão Jira: KAN-34
 * Título: Controle de acesso por dono do recurso
 * Objetivo: Responder, nas regras @PreAuthorize, se a conta logada é dona de um recurso (por exemplo, "um
 *           empreendedor só edita os empreendimentos que são dele").
 * Decisões: Bean "acesso", usado como {@code @PreAuthorize("@acesso.ehDono('startup', #id)")}. Cada módulo
 *           registra um {@link DonoDeRecurso} para o seu tipo de recurso; tipo desconhecido nega (negar por
 *           padrão). A conferência é sempre no servidor.
 */
@Component("acesso")
public class ControleDeAcesso {

    private final Map<String, DonoDeRecurso> donos;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:50
     * Cartão Jira: KAN-34
     * Título: Criação do controle
     * Objetivo: Indexar as regras de dono por tipo de recurso.
     * Decisões: Dois módulos registrando o mesmo tipo é erro de configuração e falha na inicialização.
     */
    public ControleDeAcesso(List<DonoDeRecurso> donos) {
        this.donos = donos.stream().collect(Collectors.toMap(DonoDeRecurso::tipo, Function.identity()));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:50
     * Cartão Jira: KAN-34
     * Título: É dono do recurso
     * Objetivo: Dizer se a conta logada é dona do recurso informado.
     * Decisões: Sem login, tipo desconhecido ou id nulo: falso.
     */
    public boolean ehDono(String tipo, UUID recursoId) {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (!(autenticacao instanceof JwtAuthenticationToken token) || recursoId == null) {
            return false;
        }
        DonoDeRecurso regra = donos.get(tipo);
        return regra != null && regra.ehDono(Autenticado.id(token.getToken()), recursoId);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:50
     * Cartão Jira: KAN-34
     * Título: Regra de dono de um tipo de recurso
     * Objetivo: Contrato que cada módulo implementa para o seu recurso (startup, rodada, documento...).
     * Decisões: Padrão Strategy, uma implementação por tipo.
     */
    public interface DonoDeRecurso {

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 24/11/2026
         * Hora: 01:50
         * Cartão Jira: KAN-34
         * Título: Tipo atendido
         * Objetivo: Nome do tipo de recurso usado nas regras (por exemplo, "startup").
         * Decisões: Minúsculas, sem espaço.
         */
        String tipo();

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 24/11/2026
         * Hora: 01:50
         * Cartão Jira: KAN-34
         * Título: Conferir dono
         * Objetivo: Dizer se a conta é dona do recurso.
         * Decisões: Recurso inexistente responde falso.
         */
        boolean ehDono(UUID contaId, UUID recursoId);
    }
}

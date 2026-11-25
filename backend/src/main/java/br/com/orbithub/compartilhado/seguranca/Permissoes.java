package br.com.orbithub.compartilhado.seguranca;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 01:50
 * Cartão Jira: KAN-34
 * Título: Anotações de permissão por papel
 * Objetivo: Marcar controllers e métodos que só um papel pode usar, com a regra conferida no servidor.
 * Decisões: Anotações compostas sobre @PreAuthorize, para a regra ter nome legível e não ser escrita à mão
 *           (e errada) em cada endpoint.
 */
public final class Permissoes {

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:50
     * Cartão Jira: KAN-34
     * Título: Classe de agrupamento
     * Objetivo: Impedir instâncias.
     * Decisões: Só agrupa as anotações.
     */
    private Permissoes() {
    }

    /** Só contas com papel de investidor. */
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @PreAuthorize("hasRole('INVESTIDOR')")
    public @interface SomenteInvestidor {
    }

    /** Só contas com papel de empreendedor. */
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @PreAuthorize("hasRole('EMPREENDEDOR')")
    public @interface SomenteEmpreendedor {
    }

    /** Só a equipe com papel de administrador. */
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @PreAuthorize("hasRole('ADMIN')")
    public @interface SomenteAdmin {
    }

    /** Administrador ou encarregado de dados: consulta da trilha de auditoria (KAN-80, KAN-75). */
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @PreAuthorize("hasAnyRole('ADMIN', 'ENCARREGADO_DADOS')")
    public @interface AdminOuEncarregado {
    }

    /** Analista de verificação ou administrador: fila e decisões da verificação de identidade (KAN-83). */
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALISTA_VERIFICACAO')")
    public @interface VerificacaoOuAdmin {
    }

    /** Qualquer papel da equipe interna, para ações comuns a todos os times (KAN-75). */
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALISTA_VERIFICACAO', 'CURADOR', 'MODERADOR', 'SUPORTE_N1', 'SUPORTE_N2', "
            + "'ENCARREGADO_DADOS', 'JURIDICO')")
    public @interface SomenteEquipe {
    }
}

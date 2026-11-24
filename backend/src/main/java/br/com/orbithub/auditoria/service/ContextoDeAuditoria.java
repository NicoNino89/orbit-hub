package br.com.orbithub.auditoria.service;

import java.util.Optional;
import java.util.function.UnaryOperator;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 10:00
 * Cartão Jira: KAN-80
 * Título: Descrição da ação da requisição atual
 * Objetivo: Deixar cada serviço dizer, com nome de negócio, o que a requisição fez (ação, objeto, detalhe e
 *           justificativa), para o registro de auditoria sair legível.
 * Decisões: Guardado nos atributos da requisição (RequestContextHolder), sem bean de escopo de requisição;
 *           fora de uma requisição HTTP (rotinas), as chamadas não fazem nada. Quem não descreve a ação
 *           ainda é registrado, com o método e o caminho da rota.
 */
public final class ContextoDeAuditoria {

    static final String ATRIBUTO = ContextoDeAuditoria.class.getName();

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:00
     * Cartão Jira: KAN-80
     * Título: Classe utilitária
     * Objetivo: Impedir instâncias.
     * Decisões: Só métodos estáticos.
     */
    private ContextoDeAuditoria() {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:00
     * Cartão Jira: KAN-80
     * Título: Descrever a ação
     * Objetivo: Informar ação, objeto e detalhe da requisição atual.
     * Decisões: A última descrição vale; a justificativa já informada é mantida.
     */
    public static void descrever(String acao, String objetoTipo, Object objetoId, String detalhe) {
        atualizar(d -> new Descricao(acao, objetoTipo, objetoId == null ? null : objetoId.toString(), detalhe,
                d.justificativa(), d.registrada()));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:00
     * Cartão Jira: KAN-80
     * Título: Informar a justificativa
     * Objetivo: Guardar a justificativa que o usuário interno deu para a ação.
     * Decisões: Mantém o resto da descrição.
     */
    public static void justificar(String justificativa) {
        atualizar(d -> new Descricao(d.acao(), d.objetoTipo(), d.objetoId(), d.detalhe(), justificativa,
                d.registrada()));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:00
     * Cartão Jira: KAN-80
     * Título: Marcar como já registrada
     * Objetivo: Avisar que o serviço já gravou o registro dentro da própria transação.
     * Decisões: Usado nas ações que não podem ficar sem registro (acesso a dados confidenciais): o registro
     *           é gravado antes de devolver os dados, e o interceptor não grava de novo.
     */
    public static void marcarRegistrada() {
        atualizar(d -> new Descricao(d.acao(), d.objetoTipo(), d.objetoId(), d.detalhe(), d.justificativa(), true));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:00
     * Cartão Jira: KAN-80
     * Título: Descrição atual
     * Objetivo: Ler o que os serviços informaram nesta requisição.
     * Decisões: Vazio fora de uma requisição ou quando ninguém descreveu nada.
     */
    public static Optional<Descricao> atual() {
        RequestAttributes atributos = RequestContextHolder.getRequestAttributes();
        if (atributos == null) {
            return Optional.empty();
        }
        return Optional.ofNullable((Descricao) atributos.getAttribute(ATRIBUTO, RequestAttributes.SCOPE_REQUEST));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:00
     * Cartão Jira: KAN-80
     * Título: Atualizar a descrição
     * Objetivo: Aplicar uma mudança sobre a descrição atual (ou sobre uma vazia).
     * Decisões: Sem requisição, não faz nada.
     */
    private static void atualizar(UnaryOperator<Descricao> mudanca) {
        RequestAttributes atributos = RequestContextHolder.getRequestAttributes();
        if (atributos == null) {
            return;
        }
        Descricao atual = (Descricao) atributos.getAttribute(ATRIBUTO, RequestAttributes.SCOPE_REQUEST);
        Descricao nova = mudanca.apply(atual != null ? atual : Descricao.VAZIA);
        atributos.setAttribute(ATRIBUTO, nova, RequestAttributes.SCOPE_REQUEST);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:00
     * Cartão Jira: KAN-80
     * Título: Descrição de uma ação
     * Objetivo: Agrupar o que o serviço informou.
     * Decisões: Record imutável; "registrada" indica que o serviço já gravou o registro.
     */
    public record Descricao(String acao, String objetoTipo, String objetoId, String detalhe, String justificativa,
                            boolean registrada) {

        static final Descricao VAZIA = new Descricao(null, null, null, null, null, false);
    }
}

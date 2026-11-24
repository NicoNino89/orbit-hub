package br.com.orbithub.administracao.api;

import br.com.orbithub.administracao.domain.DocumentoLegal;
import br.com.orbithub.administracao.domain.TipoDocumento;
import java.time.Instant;
import java.util.UUID;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 00:40
 * Cartão Jira: KAN-92
 * Título: Versão de documento na API
 * Objetivo: Representar uma versão de termo, política ou aviso para o front-end.
 * Decisões: Inclui a regra de aceite, para a tela saber se mostra a caixa "li e aceito".
 */
public record DocumentoLegalResponse(UUID id, TipoDocumento tipo, int versao, String titulo, String conteudo,
                                     Instant vigenteDesde, TipoDocumento.Aceite aceite) {

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:40
     * Cartão Jira: KAN-92
     * Título: Conversão da entidade
     * Objetivo: Montar a resposta a partir da versão gravada.
     * Decisões: Quem publicou não sai na resposta pública.
     */
    public static DocumentoLegalResponse de(DocumentoLegal documento) {
        return new DocumentoLegalResponse(documento.getId(), documento.getTipo(), documento.getVersao(),
                documento.getTitulo(), documento.getConteudo(), documento.getVigenteDesde(),
                documento.getTipo().aceite());
    }
}

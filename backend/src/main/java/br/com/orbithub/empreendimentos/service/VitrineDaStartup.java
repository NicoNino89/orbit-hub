package br.com.orbithub.empreendimentos.service;

import br.com.orbithub.compartilhado.armazenamento.ArmazenamentoArquivos;
import br.com.orbithub.compartilhado.erro.RecursoNaoEncontradoException;
import br.com.orbithub.compartilhado.erro.RegraDeNegocioException;
import br.com.orbithub.compartilhado.parametros.LeituraDeParametros;
import br.com.orbithub.empreendimentos.domain.PapelNaStartup;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 22:45
 * Cartão Jira: KAN-170
 * Título: Vitrine da startup
 * Objetivo: Guardar a imagem de capa e a chamada (frase curta) que aparecem no cartão da startup, e servir a capa à
 *           equipe e, publicada, a quem tem login.
 * Decisões: Fundadores editam (colaborador não). Capa em PNG, JPG ou WebP conferidos pelos bytes, até startup.capa_max_mb.
 *           Provisório: trocar a capa ou a chamada não reabre a curadoria (o conteúdo pode ser denunciado, KAN-99).
 */
@Service
public class VitrineDaStartup {

    static final String CAPA_MAX = "startup.capa_max_mb";

    private final JdbcTemplate jdbc;
    private final StartupsService startups;
    private final ArmazenamentoArquivos arquivos;
    private final LeituraDeParametros parametros;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 22:45
     * Cartão Jira: KAN-170
     * Título: Criação
     * Objetivo: Receber os serviços.
     * Decisões: Injeção por construtor.
     */
    public VitrineDaStartup(JdbcTemplate jdbc, StartupsService startups, ArmazenamentoArquivos arquivos, LeituraDeParametros parametros) {
        this.jdbc = jdbc;
        this.startups = startups;
        this.arquivos = arquivos;
        this.parametros = parametros;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 22:45
     * Cartão Jira: KAN-170
     * Título: Trocar capa
     * Objetivo: Gravar a nova capa e apagar a anterior.
     * Decisões: Tamanho e formato conferidos antes de gravar.
     */
    @Transactional
    public void trocarCapa(UUID eu, UUID startupId, byte[] conteudo) {
        exigirFundador(eu, startupId);
        long maximo = parametros.inteiro(CAPA_MAX) * 1024 * 1024;
        if (conteudo == null || conteudo.length == 0 || conteudo.length > maximo) {
            throw new RegraDeNegocioException("capa_tamanho", "A capa precisa ter até " + parametros.inteiro(CAPA_MAX) + " MB.");
        }
        String tipo = StartupsService.formatoDaImagem(conteudo).orElseThrow(() -> new RegraDeNegocioException("capa_formato",
                "Envie a capa em PNG, JPG ou WebP."));
        String chave = "startups/" + startupId + "/capa-" + UUID.randomUUID() + "." + tipo.substring(tipo.indexOf('/') + 1);
        arquivos.gravar(chave, new ByteArrayInputStream(conteudo), conteudo.length, tipo);
        String antiga = jdbc.queryForObject("SELECT capa_chave FROM startup WHERE id = ?", String.class, startupId);
        jdbc.update("UPDATE startup SET capa_chave = ?, capa_tipo = ? WHERE id = ?", chave, tipo, startupId);
        if (antiga != null) {
            arquivos.remover(antiga);
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 22:45
     * Cartão Jira: KAN-170
     * Título: Definir chamada
     * Objetivo: Gravar a frase curta do cartão (até 120 caracteres).
     * Decisões: Vazio apaga.
     */
    @Transactional
    public String definirChamada(UUID eu, UUID startupId, String chamada) {
        exigirFundador(eu, startupId);
        String texto = chamada == null || chamada.isBlank() ? null : chamada.strip();
        if (texto != null && texto.length() > 120) {
            throw new RegraDeNegocioException("chamada_longa", "Use até 120 caracteres na chamada.");
        }
        jdbc.update("UPDATE startup SET chamada = ? WHERE id = ?", texto, startupId);
        return texto;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 22:45
     * Cartão Jira: KAN-170
     * Título: Capa para a equipe
     * Objetivo: Ler a capa para quem é da startup.
     * Decisões: 404 sem capa.
     */
    @Transactional(readOnly = true)
    public Capa capaDaEquipe(UUID eu, UUID startupId) {
        startups.papel(eu, startupId);
        return capa(startupId, false);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 22:45
     * Cartão Jira: KAN-170
     * Título: Capa publicada
     * Objetivo: Ler a capa de startup publicada para o catálogo.
     * Decisões: 404 se não publicada ou sem capa.
     */
    @Transactional(readOnly = true)
    public Capa capaPublicada(UUID startupId) {
        return capa(startupId, true);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 22:45
     * Cartão Jira: KAN-170
     * Título: Vitrine
     * Objetivo: Devolver a chamada e se há capa, para a tela da equipe.
     * Decisões: Equipe da startup.
     */
    @Transactional(readOnly = true)
    public Vitrine vitrine(UUID eu, UUID startupId) {
        startups.papel(eu, startupId);
        Map<String, Object> l = jdbc.queryForMap("SELECT chamada, capa_chave FROM startup WHERE id = ?", startupId);
        return new Vitrine((String) l.get("chamada"), l.get("capa_chave") != null);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 23:59
     * Cartão Jira: KAN-167, KAN-170, KAN-171
     * Título: capa
     * Objetivo: Rotina auxiliar capa.
     * Decisões: Ver a documentação da classe.
     */
    private Capa capa(UUID startupId, boolean soPublicada) {
        List<Map<String, Object>> l = jdbc.queryForList("SELECT capa_chave, capa_tipo, situacao FROM startup WHERE id = ?", startupId);
        if (l.isEmpty() || l.get(0).get("capa_chave") == null || (soPublicada && !"PUBLICADA".equals(l.get(0).get("situacao")))) {
            throw new RecursoNaoEncontradoException("Capa não encontrada.");
        }
        return new Capa(arquivos.ler((String) l.get(0).get("capa_chave")), (String) l.get(0).get("capa_tipo"));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 23:59
     * Cartão Jira: KAN-167, KAN-170, KAN-171
     * Título: exigirFundador
     * Objetivo: Rotina auxiliar exigirFundador.
     * Decisões: Ver a documentação da classe.
     */
    private void exigirFundador(UUID eu, UUID startupId) {
        if (startups.papel(eu, startupId) == PapelNaStartup.COLABORADOR) {
            throw new AccessDeniedException("Só fundadores mudam a vitrine");
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 22:45
     * Cartão Jira: KAN-170
     * Título: Dados da vitrine
     * Objetivo: Tipar a capa e a vitrine.
     * Decisões: O conteúdo da capa é um fluxo do armazenamento.
     */
    public record Capa(InputStream conteudo, String tipo) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 23:59
     * Cartão Jira: KAN-167, KAN-170, KAN-171
     * Título: Vitrine
     * Objetivo: Rotina auxiliar Vitrine.
     * Decisões: Ver a documentação da classe.
     */
    public record Vitrine(String chamada, boolean temCapa) {
    }
}

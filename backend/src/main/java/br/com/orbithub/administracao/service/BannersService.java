package br.com.orbithub.administracao.service;

import br.com.orbithub.compartilhado.armazenamento.ArmazenamentoArquivos;
import br.com.orbithub.compartilhado.erro.RecursoNaoEncontradoException;
import br.com.orbithub.compartilhado.erro.RegraDeNegocioException;
import br.com.orbithub.empreendimentos.service.StartupsService;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 22:40
 * Cartão Jira: KAN-171
 * Título: Banners e chamadas institucionais
 * Objetivo: Deixar o administrador manter os banners rotativos e as chamadas laterais da página inicial, com público,
 *           período, ordem e imagem, e entregar às pessoas só os vigentes do papel delas.
 * Decisões: Comunicação institucional, não publicidade paga (KAN-74). Link só interno (começa com /) ou https. Imagem
 *           conferida pelos bytes, até 2 MB, com texto alternativo obrigatório quando houver imagem.
 */
@Service
public class BannersService {

    private static final Set<String> TIPOS = Set.of("BANNER", "CHAMADA");
    private static final Set<String> PUBLICOS = Set.of("TODOS", "INVESTIDOR", "EMPREENDEDOR");
    private static final int IMAGEM_MAX = 2 * 1024 * 1024;

    private final JdbcTemplate jdbc;
    private final ArmazenamentoArquivos arquivos;
    private final Clock relogio;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 22:40
     * Cartão Jira: KAN-171
     * Título: Criação
     * Objetivo: Receber os serviços.
     * Decisões: Injeção por construtor.
     */
    public BannersService(JdbcTemplate jdbc, ArmazenamentoArquivos arquivos, Clock relogio) {
        this.jdbc = jdbc;
        this.arquivos = arquivos;
        this.relogio = relogio;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 22:40
     * Cartão Jira: KAN-171
     * Título: Vigentes
     * Objetivo: Listar os banners e chamadas ativos, no período, para o papel ativo.
     * Decisões: Data de Brasília; pela ordem.
     */
    @Transactional(readOnly = true)
    public List<Banner> vigentes(String papel) {
        Date hoje = Date.valueOf(LocalDate.now(relogio.withZone(ZoneId.of("America/Sao_Paulo"))));
        return jdbc.query("SELECT * FROM banner_institucional WHERE ativo AND inicio <= ? AND (fim IS NULL OR fim >= ?) AND publico IN ('TODOS', ?) "
                + "ORDER BY tipo, ordem", (rs, i) -> banner(rs), hoje, hoje, papel == null ? "TODOS" : papel);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 22:40
     * Cartão Jira: KAN-171
     * Título: Todos
     * Objetivo: Listar todos os banners para o administrador.
     * Decisões: Pelo tipo e pela ordem.
     */
    @Transactional(readOnly = true)
    public List<Banner> todos() {
        return jdbc.query("SELECT * FROM banner_institucional ORDER BY tipo, ordem", (rs, i) -> banner(rs));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 22:40
     * Cartão Jira: KAN-171
     * Título: Salvar
     * Objetivo: Criar ou alterar um banner ou chamada.
     * Decisões: Sem id cria; com id altera.
     */
    @Transactional
    public Banner salvar(UUID eu, UUID id, Dados d) {
        conferir(d);
        UUID alvo = id == null ? UUID.randomUUID() : id;
        Timestamp agora = Timestamp.from(relogio.instant());
        Date fim = d.fim() == null ? null : Date.valueOf(d.fim());
        if (id == null) {
            jdbc.update("INSERT INTO banner_institucional (id, tipo, titulo, destaque, texto, botao, link, publico, inicio, fim, ordem, texto_alt, ativo, "
                    + "atualizado_por, atualizado_em) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", alvo, d.tipo(), d.titulo().strip(), vazio(d.destaque()),
                    vazio(d.texto()), vazio(d.botao()), vazio(d.link()), d.publico(), Date.valueOf(d.inicio()), fim, d.ordem() == null ? 1 : d.ordem(), vazio(d.textoAlt()),
                    !Boolean.FALSE.equals(d.ativo()), eu, agora);
        } else if (jdbc.update("UPDATE banner_institucional SET tipo = ?, titulo = ?, destaque = ?, texto = ?, botao = ?, link = ?, publico = ?, inicio = ?, "
                + "fim = ?, ordem = ?, texto_alt = ?, ativo = ?, atualizado_por = ?, atualizado_em = ? WHERE id = ?", d.tipo(), d.titulo().strip(),
                vazio(d.destaque()), vazio(d.texto()), vazio(d.botao()), vazio(d.link()), d.publico(), Date.valueOf(d.inicio()), fim, d.ordem() == null ? 1 : d.ordem(),
    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 22:40
     * Cartão Jira: KAN-167, KAN-170, KAN-171
     * Título: vazio
     * Objetivo: Rotina auxiliar vazio.
     * Decisões: Ver a documentação da classe.
     */
                vazio(d.textoAlt()), !Boolean.FALSE.equals(d.ativo()), eu, agora, id) == 0) {
            throw new RecursoNaoEncontradoException("Banner não encontrado.");
        }
        return ler(alvo);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 22:40
     * Cartão Jira: KAN-171
     * Título: Trocar imagem
     * Objetivo: Gravar a imagem do banner e apagar a anterior.
     * Decisões: PNG, JPG ou WebP, até 2 MB; exige texto alternativo já definido.
     */
    @Transactional
    public Banner trocarImagem(UUID eu, UUID id, byte[] conteudo) {
        Banner b = ler(id);
        if (b.textoAlt() == null) {
            throw new RegraDeNegocioException("texto_alt_obrigatorio", "Descreva a imagem (texto alternativo) antes de enviá-la.");
        }
        if (conteudo == null || conteudo.length == 0 || conteudo.length > IMAGEM_MAX) {
            throw new RegraDeNegocioException("imagem_tamanho", "A imagem precisa ter até 2 MB.");
        }
        String tipo = StartupsService.formatoDaImagem(conteudo).orElseThrow(() -> new RegraDeNegocioException("imagem_formato",
                "Envie a imagem em PNG, JPG ou WebP."));
        String chave = "banners/" + id + "-" + UUID.randomUUID() + "." + tipo.substring(tipo.indexOf('/') + 1);
        arquivos.gravar(chave, new ByteArrayInputStream(conteudo), conteudo.length, tipo);
        String antiga = jdbc.queryForObject("SELECT imagem_chave FROM banner_institucional WHERE id = ?", String.class, id);
        jdbc.update("UPDATE banner_institucional SET imagem_chave = ?, imagem_tipo = ?, atualizado_por = ?, atualizado_em = ? WHERE id = ?", chave, tipo, eu,
                Timestamp.from(relogio.instant()), id);
        if (antiga != null) {
            arquivos.remover(antiga);
        }
        return ler(id);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 22:40
     * Cartão Jira: KAN-171
     * Título: Imagem
     * Objetivo: Ler a imagem do banner.
     * Decisões: 404 sem imagem.
     */
    @Transactional(readOnly = true)
    public Imagem imagem(UUID id) {
        Map<String, Object> l = jdbc.queryForList("SELECT imagem_chave, imagem_tipo FROM banner_institucional WHERE id = ?", id).stream().findFirst()
                .orElseThrow(() -> new RecursoNaoEncontradoException("Banner não encontrado."));
        if (l.get("imagem_chave") == null) {
            throw new RecursoNaoEncontradoException("Banner sem imagem.");
        }
        return new Imagem(arquivos.ler((String) l.get("imagem_chave")), (String) l.get("imagem_tipo"));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 22:40
     * Cartão Jira: KAN-167, KAN-170, KAN-171
     * Título: ler
     * Objetivo: Rotina auxiliar ler.
     * Decisões: Ver a documentação da classe.
     */
    private Banner ler(UUID id) {
        return jdbc.query("SELECT * FROM banner_institucional WHERE id = ?", (rs, i) -> banner(rs), id).stream().findFirst()
                .orElseThrow(() -> new RecursoNaoEncontradoException("Banner não encontrado."));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 22:40
     * Cartão Jira: KAN-167, KAN-170, KAN-171
     * Título: conferir
     * Objetivo: Rotina auxiliar conferir.
     * Decisões: Ver a documentação da classe.
     */
    private static void conferir(Dados d) {
        if (d == null || !TIPOS.contains(String.valueOf(d.tipo())) || !PUBLICOS.contains(String.valueOf(d.publico()))) {
            throw new RegraDeNegocioException("banner_invalido", "Escolha o tipo e o público.");
        }
        if (d.titulo() == null || d.titulo().isBlank() || d.titulo().length() > 120) {
            throw new RegraDeNegocioException("titulo_invalido", "Dê um título com até 120 caracteres.");
        }
        if (d.inicio() == null || (d.fim() != null && d.fim().isBefore(d.inicio()))) {
            throw new RegraDeNegocioException("periodo_invalido", "Informe um período válido.");
        }
        String link = vazio(d.link());
        if (link != null && !(link.startsWith("/") && !link.startsWith("//")) && !link.startsWith("https://")) {
            throw new RegraDeNegocioException("link_invalido", "Use um endereço interno (começando com /) ou https.");
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 22:40
     * Cartão Jira: KAN-167, KAN-170, KAN-171
     * Título: banner
     * Objetivo: Rotina auxiliar banner.
     * Decisões: Ver a documentação da classe.
     */
    private static Banner banner(java.sql.ResultSet rs) throws java.sql.SQLException {
        Date fim = rs.getDate("fim");
        return new Banner(rs.getObject("id", UUID.class), rs.getString("tipo"), rs.getString("titulo"), rs.getString("destaque"), rs.getString("texto"),
                rs.getString("botao"), rs.getString("link"), rs.getString("publico"), rs.getDate("inicio").toLocalDate(),
                fim == null ? null : fim.toLocalDate(), rs.getInt("ordem"), rs.getString("imagem_chave") != null, rs.getString("texto_alt"),
                rs.getBoolean("ativo"));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 22:40
     * Cartão Jira: KAN-167, KAN-170, KAN-171
     * Título: vazio
     * Objetivo: Rotina auxiliar vazio.
     * Decisões: Ver a documentação da classe.
     */
    private static String vazio(String s) {
        return s == null || s.isBlank() ? null : s.strip();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 22:40
     * Cartão Jira: KAN-171
     * Título: Dados do banner
     * Objetivo: Tipar o pedido e a resposta.
     * Decisões: A chave da imagem nunca sai; só se há imagem.
     */
    public record Dados(String tipo, String titulo, String destaque, String texto, String botao, String link, String publico, LocalDate inicio,
                        LocalDate fim, Integer ordem, String textoAlt, Boolean ativo) {
    }

    public record Banner(UUID id, String tipo, String titulo, String destaque, String texto, String botao, String link, String publico, LocalDate inicio,
                         LocalDate fim, int ordem, boolean temImagem, String textoAlt, boolean ativo) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 22:40
     * Cartão Jira: KAN-167, KAN-170, KAN-171
     * Título: Imagem
     * Objetivo: Rotina auxiliar Imagem.
     * Decisões: Ver a documentação da classe.
     */
    public record Imagem(InputStream conteudo, String tipo) {
    }
}

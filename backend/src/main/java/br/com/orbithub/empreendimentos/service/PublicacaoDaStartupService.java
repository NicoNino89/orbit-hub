package br.com.orbithub.empreendimentos.service;

import br.com.orbithub.compartilhado.erro.RecursoNaoEncontradoException;
import br.com.orbithub.compartilhado.erro.RegraDeNegocioException;
import br.com.orbithub.empreendimentos.domain.CampoDaStartup;
import br.com.orbithub.empreendimentos.domain.MudancaDeSituacao;
import br.com.orbithub.empreendimentos.domain.RevisaoDeCuradoria;
import br.com.orbithub.empreendimentos.domain.SecaoDaStartup;
import br.com.orbithub.empreendimentos.domain.SituacaoDaStartup;
import br.com.orbithub.empreendimentos.domain.Startup;
import br.com.orbithub.empreendimentos.repository.MudancaDeSituacaoRepository;
import br.com.orbithub.empreendimentos.repository.StartupRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 23:40
 * Cartão Jira: KAN-27
 * Título: Publicação da startup
 * Objetivo: Mostrar o progresso do cadastro, enviar a startup para a revisão da plataforma, cancelar o envio e
 *           despublicar, registrando cada mudança de situação.
 * Decisões: O envio só é liberado com os obrigatórios preenchidos (nome, segmento e localização, ao menos um fundador e
 *           os campos marcados no catálogo). Só o fundador administrador envia, cancela e despublica (KAN-65). A decisão
 *           da revisão (publicar ou devolver) é da curadoria (KAN-66). A edição continua liberada em qualquer situação.
 */
@Service
public class PublicacaoDaStartupService {

    private final StartupRepository startups;
    private final MudancaDeSituacaoRepository historico;
    private final StartupsService cadastro;
    private final Clock relogio;
    private final RevisoesDaStartup revisoes;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:40
     * Cartão Jira: KAN-27
     * Título: Criação do serviço
     * Objetivo: Receber os repositórios, o cadastro, o relógio e as revisões da curadoria.
     * Decisões: Injeção por construtor. As revisões entram no KAN-66 (25/11/2026).
     */
    public PublicacaoDaStartupService(StartupRepository startups, MudancaDeSituacaoRepository historico, StartupsService cadastro,
                                      Clock relogio, RevisoesDaStartup revisoes) {
        this.revisoes = revisoes;
        this.startups = startups;
        this.historico = historico;
        this.cadastro = cadastro;
        this.relogio = relogio;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:40
     * Cartão Jira: KAN-27
     * Título: Progresso
     * Objetivo: Calcular, por seção, quantos campos estão preenchidos, o percentual dos obrigatórios e o que falta para
     *           enviar.
     * Decisões: Qualquer membro da equipe vê. O percentual conta só os obrigatórios, para o 100% significar "pode enviar".
     */
    @Transactional(readOnly = true)
    public Progresso progresso(UUID eu, UUID startupId) {
        cadastro.papel(eu, startupId);
        return progresso(startup(startupId));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:40
     * Cartão Jira: KAN-27
     * Título: Enviar para publicação
     * Objetivo: Passar a startup para a revisão da plataforma.
     * Decisões: Recusa com a lista do que falta. Abre a revisão da curadoria (KAN-66). Startup publicada com alteração
     *           relevante devolvida pela curadoria reenvia a alteração e continua publicada (25/11/2026).
     */
    @Transactional
    public SituacaoDaStartup enviar(UUID eu, UUID startupId) {
        cadastro.exigirAdministrador(eu, startupId);
        Startup startup = startup(startupId);
        Progresso p = progresso(startup);
        if (!p.podeEnviar()) {
            throw new RegraDeNegocioException("cadastro_incompleto", "Preencha antes de enviar: " + String.join(", ", p.pendencias()) + ".");
        }
        if (startup.getSituacao() == SituacaoDaStartup.PUBLICADA) {
            if (!startup.temAlteracaoRelevante()) {
                throw new RegraDeNegocioException("sem_alteracao", "Não há alteração de nome, segmento ou estágio para a curadoria revisar.");
            }
            revisoes.abrir(startup, RevisaoDeCuradoria.Tipo.ALTERACAO, eu);
            return startup.getSituacao();
        }
        SituacaoDaStartup anterior = startup.enviarParaRevisao(relogio.instant());
        revisoes.abrir(startup, RevisaoDeCuradoria.Tipo.PUBLICACAO, eu);
        return registrar(startup, anterior, eu, null);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:40
     * Cartão Jira: KAN-27
     * Título: Cancelar envio
     * Objetivo: Tirar a startup da revisão antes da decisão da curadoria.
     * Decisões: Só o administrador.
     */
    @Transactional
    public SituacaoDaStartup cancelarEnvio(UUID eu, UUID startupId) {
        cadastro.exigirAdministrador(eu, startupId);
        Startup startup = startup(startupId);
        SituacaoDaStartup anterior = startup.cancelarEnvio(relogio.instant());
        revisoes.cancelar(startupId);
        return registrar(startup, anterior, eu, null);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:40
     * Cartão Jira: KAN-27
     * Título: Despublicar
     * Objetivo: Tirar a startup do catálogo.
     * Decisões: Só o administrador. Conexões e rodadas em andamento continuam visíveis para as partes (S5 e S7 leem a
     *           situação, não apagam nada).
     */
    @Transactional
    public SituacaoDaStartup despublicar(UUID eu, UUID startupId, String motivo) {
        cadastro.exigirAdministrador(eu, startupId);
        Startup startup = startup(startupId);
        revisoes.cancelar(startupId);
        return registrar(startup, startup.despublicar(relogio.instant()), eu, motivo == null || motivo.isBlank() ? null : motivo.strip());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:40
     * Cartão Jira: KAN-27
     * Título: Histórico
     * Objetivo: Listar as mudanças de situação da startup.
     * Decisões: Qualquer membro da equipe vê.
     */
    @Transactional(readOnly = true)
    public List<MudancaDeSituacao> historico(UUID eu, UUID startupId) {
        cadastro.papel(eu, startupId);
        return historico.findByStartupIdOrderByEmDesc(startupId);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:40
     * Cartão Jira: KAN-27
     * Título: Despublicar por inatividade
     * Objetivo: Despublicar a startup em nome da R8, registrando o motivo.
     * Decisões: Sem autor (rotina). Pacote, para a rotina.
     */
    void despublicarPorInatividade(Startup startup, Instant agora) {
        registrar(startup, startup.despublicar(agora), null, "Sem atualização depois do aviso de inatividade (R8)");
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:40
     * Cartão Jira: KAN-27
     * Título: Registrar mudança
     * Objetivo: Gravar a mudança de situação no histórico e devolver a situação nova.
     * Decisões: Mesma transação da mudança.
     */
    private SituacaoDaStartup registrar(Startup startup, SituacaoDaStartup anterior, UUID por, String motivo) {
        historico.save(MudancaDeSituacao.nova(startup.getId(), anterior, startup.getSituacao(), por, motivo, relogio.instant()));
        return startup.getSituacao();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:40
     * Cartão Jira: KAN-27
     * Título: Calcular progresso
     * Objetivo: Montar o progresso a partir dos dados cadastrais e dos campos das seções.
     * Decisões: Dados cadastrais contam nome, segmento e localização (sempre preenchidos) e ao menos um fundador.
     */
    private static Progresso progresso(Startup startup) {
        Map<CampoDaStartup, String> campos = startup.campos();
        List<String> pendencias = new ArrayList<>();
        boolean temFundador = !startup.cadastro().fundadores().isEmpty();
        int obrigatorios = 1;
        int cumpridos = temFundador ? 1 : 0;
        if (!temFundador) {
            pendencias.add("ao menos um fundador nos dados cadastrais");
        }
        List<Secao> secoes = new ArrayList<>();
        secoes.add(new Secao(SecaoDaStartup.CADASTRO, temFundador ? 1 : 0, 1, temFundador));
        for (SecaoDaStartup secao : SecaoDaStartup.values()) {
            List<CampoDaStartup> daSecao = CampoDaStartup.da(secao);
            if (secao == SecaoDaStartup.CADASTRO || daSecao.isEmpty()) {
                continue;
            }
            int preenchidos = 0;
            boolean completa = true;
            for (CampoDaStartup c : daSecao) {
                boolean tem = campos.containsKey(c);
                preenchidos += tem ? 1 : 0;
                if (c.obrigatorioParaPublicar()) {
                    obrigatorios++;
                    cumpridos += tem ? 1 : 0;
                    if (!tem) {
                        completa = false;
                        pendencias.add(c.getRotulo().toLowerCase(Locale.ROOT));
                    }
                }
            }
            secoes.add(new Secao(secao, preenchidos, daSecao.size(), completa));
        }
        return new Progresso(cumpridos * 100 / obrigatorios, pendencias.isEmpty(), pendencias, secoes, startup.getSituacao());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:40
     * Cartão Jira: KAN-27
     * Título: Startup
     * Objetivo: Ler a startup pelo identificador.
     * Decisões: 404 se não existir.
     */
    private Startup startup(UUID id) {
        return startups.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Startup não encontrada."));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:40
     * Cartão Jira: KAN-27
     * Título: Progresso do cadastro
     * Objetivo: Levar o percentual dos obrigatórios, se pode enviar, o que falta e o estado de cada seção.
     * Decisões: Inclui a situação atual.
     */
    public record Progresso(int percentual, boolean podeEnviar, List<String> pendencias, List<Secao> secoes, SituacaoDaStartup situacao) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:40
     * Cartão Jira: KAN-27
     * Título: Seção no progresso
     * Objetivo: Levar quantos campos da seção estão preenchidos e se os obrigatórios dela estão completos.
     * Decisões: Completa = sem obrigatório faltando.
     */
    public record Secao(SecaoDaStartup secao, int preenchidos, int total, boolean completa) {
    }
}

package br.com.orbithub.auditoria.service;

import br.com.orbithub.auditoria.domain.RegistroAuditoria;
import br.com.orbithub.auditoria.domain.RegistroAuditoria.Acao;
import br.com.orbithub.auditoria.domain.RegistroAuditoria.Resultado;
import br.com.orbithub.auditoria.repository.RegistroAuditoriaRepository;
import br.com.orbithub.compartilhado.erro.RegraDeNegocioException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 10:10
 * Cartão Jira: KAN-80
 * Título: Acesso justificado da equipe a dados confidenciais
 * Objetivo: Só deixar um usuário interno ver dados de Nível 2 de uma startup dentro de um caso aberto
 *           (denúncia, disputa, solicitação do titular), com justificativa, e deixar esse acesso registrado.
 * Decisões: Cada módulo que tem casos registra um {@link CasoAberto} (Strategy, como o DonoDeRecurso da
 *           KAN-34). Sem caso registrado para o tipo, o acesso é negado (negar por padrão). O registro é
 *           gravado antes de os dados saírem, na mesma transação: se a gravação falhar, os dados não saem.
 *           O histórico de acessos da startup (KAN-33) lê {@link #acessosInternos} e mostra o autor como
 *           "equipe Orbit Hub", sem expor o nome da pessoa.
 */
@Service
public class AcessoJustificadoService {

    public static final String ACAO = "NIVEL2_ACESSO_INTERNO";
    public static final String OBJETO = "startup";
    public static final String AUTOR_EXIBIDO = "equipe Orbit Hub";
    static final int JUSTIFICATIVA_MINIMA = 20;

    private final Map<String, CasoAberto> casos;
    private final TrilhaDeAuditoria trilha;
    private final RegistroAuditoriaRepository registros;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:10
     * Cartão Jira: KAN-80
     * Título: Criação do serviço
     * Objetivo: Indexar as regras de caso aberto por tipo de caso.
     * Decisões: Dois módulos com o mesmo tipo de caso é erro de configuração e falha na inicialização.
     */
    public AcessoJustificadoService(List<CasoAberto> casos, TrilhaDeAuditoria trilha,
                                    RegistroAuditoriaRepository registros) {
        this.casos = casos.stream().collect(Collectors.toMap(CasoAberto::tipo, Function.identity()));
        this.trilha = trilha;
        this.registros = registros;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:10
     * Cartão Jira: KAN-80
     * Título: Exigir e registrar o acesso
     * Objetivo: Conferir caso e justificativa e gravar o acesso antes de liberar os dados.
     * Decisões: Justificativa com pelo menos 20 caracteres, para não aceitar "ok"; o caso precisa estar
     *           aberto e envolver a startup. A ação é descrita antes das validações, para a recusa também
     *           entrar na trilha com o mesmo nome; no sucesso, avisa que o registro já foi gravado.
     */
    @Transactional
    public RegistroAuditoria exigir(UUID atorId, String casoTipo, UUID casoId, UUID startupId, String justificativa) {
        String detalhe = "Caso " + casoTipo + " " + casoId;
        ContextoDeAuditoria.descrever(ACAO, OBJETO, startupId, detalhe);
        ContextoDeAuditoria.justificar(justificativa);
        String texto = justificativa == null ? "" : justificativa.strip();
        if (texto.length() < JUSTIFICATIVA_MINIMA) {
            throw new RegraDeNegocioException("justificativa_obrigatoria",
                    "Explique por que precisa ver estes dados (pelo menos " + JUSTIFICATIVA_MINIMA + " caracteres).");
        }
        CasoAberto regra = casos.get(casoTipo);
        if (regra == null || casoId == null || !regra.abertoEnvolvendo(casoId, startupId)) {
            throw new RegraDeNegocioException("caso_nao_aberto",
                    "O acesso a dados confidenciais só é liberado dentro de um caso aberto sobre esta startup.");
        }
        RegistroAuditoria registro = trilha.registrar(new Acao(atorId, null, ACAO, OBJETO, startupId.toString(),
                Resultado.SUCESSO, null, detalhe, texto));
        ContextoDeAuditoria.marcarRegistrada();
        return registro;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:10
     * Cartão Jira: KAN-80
     * Título: Acessos internos a uma startup
     * Objetivo: Listar os acessos da equipe aos dados confidenciais da startup, para o histórico de acessos.
     * Decisões: O autor sai como "equipe Orbit Hub"; o nome real fica só na trilha, para o administrador e
     *           o encarregado de dados.
     */
    @Transactional(readOnly = true)
    public List<AcessoInterno> acessosInternos(UUID startupId) {
        return registros.findByAcaoAndObjetoTipoAndObjetoIdAndResultadoOrderByOcorridoEmDesc(
                        ACAO, OBJETO, startupId.toString(), Resultado.SUCESSO).stream()
                .map(r -> new AcessoInterno(AUTOR_EXIBIDO, r.getOcorridoEm(), r.getDetalhe()))
                .toList();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:10
     * Cartão Jira: KAN-80
     * Título: Acesso interno exibido à startup
     * Objetivo: Representar um acesso da equipe no histórico de acessos da startup.
     * Decisões: Sem nome nem id da pessoa da equipe.
     */
    public record AcessoInterno(String autor, Instant ocorridoEm, String motivo) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:10
     * Cartão Jira: KAN-80
     * Título: Regra de caso aberto
     * Objetivo: Contrato que cada módulo com casos implementa (moderação, disputas, LGPD).
     * Decisões: Padrão Strategy, uma implementação por tipo de caso.
     */
    public interface CasoAberto {

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 24/11/2026
         * Hora: 10:10
         * Cartão Jira: KAN-80
         * Título: Tipo de caso
         * Objetivo: Nome do tipo de caso (por exemplo, "denuncia", "disputa", "titular").
         * Decisões: Minúsculas, sem espaço.
         */
        String tipo();

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 24/11/2026
         * Hora: 10:10
         * Cartão Jira: KAN-80
         * Título: Caso aberto sobre a startup
         * Objetivo: Dizer se o caso existe, está aberto e envolve a startup.
         * Decisões: Caso inexistente ou encerrado responde falso.
         */
        boolean abertoEnvolvendo(UUID casoId, UUID startupId);
    }
}

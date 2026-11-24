package br.com.orbithub.investidores.api;

import br.com.orbithub.compartilhado.escalas.MaturidadeDoNegocio;
import br.com.orbithub.compartilhado.seguranca.Autenticado;
import br.com.orbithub.compartilhado.seguranca.Permissoes.SomenteInvestidor;
import br.com.orbithub.investidores.domain.PerfilDeInvestimento;
import br.com.orbithub.investidores.domain.PerfilDeInvestimento.Lideranca;
import br.com.orbithub.investidores.domain.PerfilDeInvestimento.ParticipacaoNaGestao;
import br.com.orbithub.investidores.service.AtuacaoService;
import br.com.orbithub.investidores.service.PerfilDeInvestimentoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 14:16
 * Cartão Jira: KAN-61
 * Título: Perfil de investimento
 * Objetivo: Ler e salvar o perfil de investimento de quem atua (a pessoa ou a organização escolhida).
 * Decisões: Só investidores. Ler aceita qualquer membro; salvar usa a atuação "para agir" e o serviço exige o
 *           administrador quando o perfil é da organização.
 */
@RestController
@RequestMapping("/api/v1/investidores/perfil")
@SomenteInvestidor
@Tag(name = "Investidores", description = "Funções de quem atua como investidor")
class PerfilDeInvestimentoController {

    private final PerfilDeInvestimentoService perfis;
    private final AtuacaoService atuacao;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:16
     * Cartão Jira: KAN-61
     * Título: Criação do controller
     * Objetivo: Receber os serviços de perfil e de atuação.
     * Decisões: Injeção por construtor.
     */
    PerfilDeInvestimentoController(PerfilDeInvestimentoService perfis, AtuacaoService atuacao) {
        this.perfis = perfis;
        this.atuacao = atuacao;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:16
     * Cartão Jira: KAN-61
     * Título: Ler o perfil
     * Objetivo: Devolver o perfil de quem atua e o que pode ser feito com ele.
     * Decisões: Sem perfil ainda: "perfil" nulo.
     */
    @GetMapping
    @Operation(summary = "Perfil de investimento de quem atua")
    PerfilResponse ler(@RequestHeader(name = AtuacaoService.CABECALHO, required = false) String organizacao,
                       @AuthenticationPrincipal Jwt token) {
        return PerfilResponse.de(perfis.ler(atuacao.paraVer(Autenticado.id(token), organizacao)));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:16
     * Cartão Jira: KAN-61
     * Título: Salvar o perfil
     * Objetivo: Criar ou atualizar o perfil de quem atua.
     * Decisões: Erros de regra em 422; organização sem ser administrador em 403.
     */
    @PutMapping
    @Operation(summary = "Salva o perfil de investimento de quem atua")
    PerfilResponse salvar(@RequestHeader(name = AtuacaoService.CABECALHO, required = false) String organizacao,
                          @Valid @RequestBody PerfilRequest pedido, @AuthenticationPrincipal Jwt token) {
        return PerfilResponse.de(perfis.salvar(atuacao.paraAgir(Autenticado.id(token), organizacao), pedido.dados()));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:25
     * Cartão Jira: KAN-18
     * Título: Pedido do perfil
     * Objetivo: Receber os campos do perfil, inclusive a tese.
     * Decisões: As regras de conteúdo ficam no serviço; aqui só a presença do follow-on.
     */
    record PerfilRequest(String tipo, String tipoOutro, BigDecimal ticketMinimo, BigDecimal ticketMaximo,
                         Lideranca lideranca,
                         @NotNull(message = "Informe se reserva capital para follow-on.") Boolean followOn,
                         ParticipacaoNaGestao participacaoNaGestao, Set<String> instrumentos, Set<String> segmentos,
                         Set<String> estagios, Set<MaturidadeDoNegocio> maturidades, Set<String> localizacoes) {

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 24/11/2026
         * Hora: 14:25
         * Cartão Jira: KAN-18
         * Título: Para o serviço
         * Objetivo: Converter o pedido nos dados do perfil.
         * Decisões: Copia os campos.
         */
        PerfilDeInvestimento.Dados dados() {
            return new PerfilDeInvestimento.Dados(tipo, tipoOutro, ticketMinimo, ticketMaximo, lideranca, followOn,
                    participacaoNaGestao, instrumentos, segmentos, estagios, maturidades, localizacoes);
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:16
     * Cartão Jira: KAN-61
     * Título: Perfil na resposta
     * Objetivo: Levar o dono, se pode editar, o tipo fixo, o perfil e a última alteração.
     * Decisões: Campos opcionais viram nulo.
     */
    record PerfilResponse(boolean daOrganizacao, boolean podeEditar, String tipoFixo, PerfilDeInvestimento.Dados perfil,
                          String atualizadoPor, Instant atualizadoEm) {

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 24/11/2026
         * Hora: 14:16
         * Cartão Jira: KAN-61
         * Título: Conversão
         * Objetivo: Montar a resposta a partir da situação do serviço.
         * Decisões: Optional vira nulo.
         */
        static PerfilResponse de(PerfilDeInvestimentoService.Situacao s) {
            return new PerfilResponse(s.daOrganizacao(), s.podeEditar(), s.tipoFixo(), s.perfil().orElse(null),
                    s.atualizadoPor().orElse(null), s.atualizadoEm().orElse(null));
        }
    }
}

package br.com.orbithub.identidade.service;

import br.com.orbithub.auditoria.domain.RegistroAuditoria.Acao;
import br.com.orbithub.auditoria.domain.RegistroAuditoria.Resultado;
import br.com.orbithub.compartilhado.parametros.Chaves;
import br.com.orbithub.compartilhado.parametros.LeituraDeParametros;
import br.com.orbithub.auditoria.service.TrilhaDeAuditoria;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 11:08
 * Cartão Jira: KAN-78
 * Título: Criação do primeiro administrador na implantação
 * Objetivo: Criar o primeiro administrador da plataforma por procedimento técnico, já que só um administrador
 *           cadastra a equipe (docs/operacao/primeiro-administrador.md).
 * Decisões: Roda na subida da API, só com ORBIT_PRIMEIRO_ADMIN_EMAIL preenchido e enquanto não houver administrador
 *           ativo; depois disso não faz mais nada, mesmo com a variável preenchida. O convite vai por e-mail; sem
 *           provedor de e-mail (modo "registro"), o link de uso único aparece uma vez no log, porque é a única forma
 *           de quem implanta recebê-lo. Quem lê o log da API já administra a infraestrutura. A criação entra na
 *           trilha de auditoria como ação do sistema.
 */
@Component
class PrimeiroAdministrador implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(PrimeiroAdministrador.class);
    static final String ACAO = "EQUIPE_PRIMEIRO_ADMINISTRADOR_CONVIDADO";

    private final EquipeService equipe;
    private final EquipeProperties propriedades;
    private final TrilhaDeAuditoria trilha;
    private final LeituraDeParametros parametros;
    private final String tipoDeEmail;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:08
     * Cartão Jira: KAN-78
     * Título: Criação do procedimento
     * Objetivo: Receber o serviço da equipe, a configuração, a trilha e o tipo de envio de e-mail.
     * Decisões: O tipo de e-mail decide se o link vai para o log.
     */
    PrimeiroAdministrador(EquipeService equipe, EquipeProperties propriedades, TrilhaDeAuditoria trilha,
                          LeituraDeParametros parametros,
                          @Value("${orbit.email.tipo}") String tipoDeEmail) {
        this.equipe = equipe;
        this.propriedades = propriedades;
        this.trilha = trilha;
        this.parametros = parametros;
        this.tipoDeEmail = tipoDeEmail;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:08
     * Cartão Jira: KAN-78
     * Título: Executar na subida
     * Objetivo: Garantir o primeiro administrador quando configurado.
     * Decisões: Falha de configuração (e-mail de conta pública) não derruba a API: vai para o log como erro.
     */
    @Override
    public void run(ApplicationArguments argumentos) {
        EquipeProperties.PrimeiroAdministrador dados = propriedades.primeiroAdministrador();
        if (dados == null || dados.email() == null || dados.email().isBlank()) {
            return;
        }
        try {
            equipe.garantirPrimeiroAdministrador(dados.nome(), dados.email()).ifPresentOrElse(link -> {
                trilha.registrar(new Acao(null, null, ACAO, "conta", null, Resultado.SUCESSO, null,
                        "Convite do primeiro administrador emitido na implantação", null));
                if ("registro".equals(tipoDeEmail)) {
                    LOG.warn("Primeiro administrador: sem provedor de e-mail, abra este link de uso único para "
                            + "definir a senha (vale {} dias): {}", parametros.duracao(Chaves.CONVITE_VALIDADE).toDays(), link);
                } else {
                    LOG.info("Primeiro administrador: convite enviado por e-mail.");
                }
            }, () -> LOG.info("Primeiro administrador: já existe administrador ativo; nada a fazer."));
        } catch (IllegalStateException conflito) {
            LOG.error("Primeiro administrador não criado: {}", conflito.getMessage());
        }
    }
}

package br.com.orbithub.empreendimentos.service;

import br.com.orbithub.administracao.service.ListasService;
import br.com.orbithub.compartilhado.erro.RecursoNaoEncontradoException;
import br.com.orbithub.compartilhado.erro.RegraDeNegocioException;
import br.com.orbithub.compartilhado.escalas.MaturidadeDoNegocio;
import br.com.orbithub.empreendimentos.domain.CampoDaStartup;
import br.com.orbithub.empreendimentos.domain.SecaoDaStartup;
import br.com.orbithub.empreendimentos.domain.Startup;
import br.com.orbithub.empreendimentos.repository.StartupRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 22:56
 * Cartão Jira: KAN-22
 * Título: Seções descritivas da startup
 * Objetivo: Ler e salvar os campos das seções do cadastro (problema e solução, produto e as seguintes), conferindo cada
 *           valor pelo catálogo.
 * Decisões: Salva uma seção por vez, para o colaborador editar só a dele (StartupsService.exigirEditor). Campo vazio é
 *           apagado. Os campos não são obrigatórios para salvar; o que é preciso para publicar é conferido no envio
 *           (KAN-27).
 */
@Service
public class SecoesDaStartupService {

    private static final BigDecimal CEM = BigDecimal.valueOf(100);

    private final StartupRepository startups;
    private final StartupsService cadastro;
    private final ListasService listas;
    private final Clock relogio;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:02
     * Cartão Jira: KAN-23
     * Título: Criação do serviço
     * Objetivo: Receber o repositório, o cadastro, as listas e o relógio.
     * Decisões: Injeção por construtor.
     */
    public SecoesDaStartupService(StartupRepository startups, StartupsService cadastro, ListasService listas, Clock relogio) {
        this.startups = startups;
        this.cadastro = cadastro;
        this.listas = listas;
        this.relogio = relogio;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:56
     * Cartão Jira: KAN-22
     * Título: Ler seções
     * Objetivo: Devolver os campos preenchidos da startup.
     * Decisões: Qualquer membro da equipe lê; quem é de fora recebe 404.
     */
    @Transactional(readOnly = true)
    public Map<CampoDaStartup, String> ler(UUID eu, UUID startupId) {
        cadastro.papel(eu, startupId);
        return startup(startupId).campos();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:26
     * Cartão Jira: KAN-25
     * Título: Salvar seção
     * Objetivo: Conferir e gravar os valores de uma seção.
     * Decisões: Campo desconhecido ou de outra seção é recusado, para não gravar o que a tela não mostra. Os percentuais
     *           de uma seção (destinação dos recursos) somam no máximo 100% (KAN-25).
     */
    @Transactional
    public Map<CampoDaStartup, String> salvar(UUID eu, UUID startupId, SecaoDaStartup secao, Map<String, String> valores) {
        if (secao == SecaoDaStartup.CADASTRO || CampoDaStartup.da(secao).isEmpty()) {
            throw new RegraDeNegocioException("secao_invalida", "Esta seção não é editada por aqui.");
        }
        cadastro.exigirEditor(eu, startupId, secao);
        Map<CampoDaStartup, String> conferidos = new EnumMap<>(CampoDaStartup.class);
        (valores == null ? Map.<String, String>of() : valores).forEach((nome, valor) -> {
            CampoDaStartup campo = campo(nome, secao);
            if (valor != null && !valor.isBlank()) {
                conferidos.put(campo, conferir(campo, valor.strip()));
            }
        });
        BigDecimal soma = conferidos.entrySet().stream().filter(e -> e.getKey().getTipo() == CampoDaStartup.Tipo.PERCENTUAL)
                .map(e -> new BigDecimal(e.getValue())).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (soma.compareTo(CEM) > 0) {
            throw new RegraDeNegocioException("percentuais_acima", "A soma das destinações passa de 100% (" + soma.stripTrailingZeros().toPlainString() + "%).");
        }
        Startup startup = startup(startupId);
        startup.preencherSecao(secao, conferidos, relogio.instant());
        return startup.campos();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:30
     * Cartão Jira: KAN-26
     * Título: Estágio da startup
     * Objetivo: Devolver o estágio do negócio escolhido, para o envio à publicação (KAN-27) e a compatibilidade com a tese do investidor.
     * Decisões: Vazio quando ainda não foi escolhido. Serviço público, sem conferir a equipe.
     */
    @Transactional(readOnly = true)
    public Optional<MaturidadeDoNegocio> estagio(UUID startupId) {
        return Optional.ofNullable(startup(startupId).campos().get(CampoDaStartup.ESTAGIO)).map(MaturidadeDoNegocio::valueOf);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:56
     * Cartão Jira: KAN-22
     * Título: Campo da seção
     * Objetivo: Converter o nome recebido no campo do catálogo.
     * Decisões: Precisa ser da seção salva.
     */
    private static CampoDaStartup campo(String nome, SecaoDaStartup secao) {
        try {
            CampoDaStartup campo = CampoDaStartup.valueOf(nome);
            if (campo.getSecao() == secao) {
                return campo;
            }
        } catch (IllegalArgumentException | NullPointerException e) {
            // cai na recusa abaixo
        }
        throw new RegraDeNegocioException("campo_invalido", "O campo " + nome + " não faz parte desta seção.");
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:30
     * Cartão Jira: KAN-26
     * Título: Conferir valor
     * Objetivo: Validar o valor conforme o tipo do campo e devolvê-lo normalizado.
     * Decisões: Texto pelo limite; sim/não; opção ativa da lista (KAN-23); percentual de 0 a 100 e inteiro de 1 ao limite
     *           (KAN-25); nível da escala de maturidade (KAN-26). Vírgula decimal aceita.
     */
    String conferir(CampoDaStartup campo, String valor) {
        return switch (campo.getTipo()) {
            case TEXTO -> {
                if (valor.length() > campo.getLimite()) {
                    throw new RegraDeNegocioException("texto_longo",
                            "O campo \"" + campo.getRotulo() + "\" pode ter até " + campo.getLimite() + " caracteres.");
                }
                yield valor;
            }
            case SIM_NAO -> {
                if (!valor.equals("SIM") && !valor.equals("NAO")) {
                    throw new RegraDeNegocioException("valor_invalido", "Responda sim ou não em \"" + campo.getRotulo() + "\".");
                }
                yield valor;
            }
            case LISTA -> listas.exigirAtivo(campo.getLista(), valor).getCodigo();
            case PERCENTUAL -> {
                BigDecimal n = numero(campo, valor);
                if (n.compareTo(BigDecimal.ZERO) < 0 || n.compareTo(CEM) > 0 || Math.max(n.stripTrailingZeros().scale(), 0) > 2) {
                    throw new RegraDeNegocioException("numero_invalido", "Informe um percentual de 0 a 100 em \"" + campo.getRotulo() + "\".");
                }
                yield n.stripTrailingZeros().toPlainString();
            }
            case INTEIRO -> {
                BigDecimal n = numero(campo, valor);
                if (n.stripTrailingZeros().scale() > 0 || n.compareTo(BigDecimal.ONE) < 0 || n.compareTo(BigDecimal.valueOf(campo.getLimite())) > 0) {
                    throw new RegraDeNegocioException("numero_invalido",
                            "Informe um número inteiro de 1 a " + campo.getLimite() + " em \"" + campo.getRotulo() + "\".");
                }
                yield n.toBigInteger().toString();
            }
            case ESCALA -> {
                try {
                    yield MaturidadeDoNegocio.valueOf(valor).name();
                } catch (IllegalArgumentException e) {
                    throw new RegraDeNegocioException("opcao_indisponivel", "Escolha um dos estágios da escala.");
                }
            }
        };
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:26
     * Cartão Jira: KAN-25
     * Título: Número
     * Objetivo: Ler o número digitado num campo numérico.
     * Decisões: Aceita vírgula decimal.
     */
    private static BigDecimal numero(CampoDaStartup campo, String valor) {
        try {
            return new BigDecimal(valor.replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new RegraDeNegocioException("numero_invalido", "Informe um número em \"" + campo.getRotulo() + "\".");
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:56
     * Cartão Jira: KAN-22
     * Título: Startup
     * Objetivo: Ler a startup pelo identificador.
     * Decisões: 404 se não existir.
     */
    private Startup startup(UUID id) {
        return startups.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Startup não encontrada."));
    }
}

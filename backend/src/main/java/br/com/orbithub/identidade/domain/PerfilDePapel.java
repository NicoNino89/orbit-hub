package br.com.orbithub.identidade.domain;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 01:00
 * Cartão Jira: KAN-12
 * Título: Perfil de um papel público
 * Objetivo: Descrever o que muda para a conta ao ter um papel: o onboarding e os termos exigidos.
 * Decisões: Padrão Factory Method (docs/arquitetura.md): o cadastro e a ativação de papel (KAN-16) pedem
 *           o perfil pela fábrica {@link #para(Papel)} e não conhecem as classes concretas. Um papel novo
 *           entra criando um perfil, sem mexer no cadastro.
 */
public sealed interface PerfilDePapel permits PerfilDePapel.Investidor, PerfilDePapel.Empreendedor {

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:35
     * Cartão Jira: KAN-75
     * Título: Fábrica de perfis
     * Objetivo: Criar o perfil correspondente ao papel.
     * Decisões: Papel interno não tem perfil público: recusado com IllegalArgumentException. O caso padrão
     *           cobre todos os papéis internos, inclusive os que vierem depois (KAN-75).
     */
    static PerfilDePapel para(Papel papel) {
        return switch (papel) {
            case INVESTIDOR -> new Investidor();
            case EMPREENDEDOR -> new Empreendedor();
            default -> throw new IllegalArgumentException(papel + " não é um papel de cadastro");
        };
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:00
     * Cartão Jira: KAN-12
     * Título: Papel do perfil
     * Objetivo: Informar qual papel o perfil representa.
     * Decisões: Um perfil por papel.
     */
    Papel papel();

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:00
     * Cartão Jira: KAN-12
     * Título: Onboarding do papel
     * Objetivo: Informar a rota da tela de onboarding do papel no front-end.
     * Decisões: Tese de investimento (KAN-18) ou cadastro da startup (KAN-21).
     */
    String rotaOnboarding();

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:00
     * Cartão Jira: KAN-12
     * Título: Exige confidencialidade
     * Objetivo: Dizer se o papel exige o aceite do Termo de Confidencialidade ao ser ativado.
     * Decisões: Só o investidor, que pode acessar informação de Nível 2.
     */
    boolean exigeTermoDeConfidencialidade();

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:00
     * Cartão Jira: KAN-12
     * Título: Perfil de investidor
     * Objetivo: Onboarding pela tese de investimento e aceite da confidencialidade.
     * Decisões: Record imutável, sem estado.
     */
    record Investidor() implements PerfilDePapel {

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 24/11/2026
         * Hora: 01:00
         * Cartão Jira: KAN-12
         * Título: Papel do investidor
         * Objetivo: Devolver INVESTIDOR.
         * Decisões: Fixo.
         */
        @Override
        public Papel papel() {
            return Papel.INVESTIDOR;
        }

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 24/11/2026
         * Hora: 01:00
         * Cartão Jira: KAN-12
         * Título: Onboarding do investidor
         * Objetivo: Rota da tese de investimento.
         * Decisões: A tela chega com o KAN-18 (S3).
         */
        @Override
        public String rotaOnboarding() {
            return "/investidor/onboarding";
        }

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 24/11/2026
         * Hora: 01:00
         * Cartão Jira: KAN-12
         * Título: Confidencialidade do investidor
         * Objetivo: Exigir o Termo de Confidencialidade.
         * Decisões: Regra do KAN-12.
         */
        @Override
        public boolean exigeTermoDeConfidencialidade() {
            return true;
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:00
     * Cartão Jira: KAN-12
     * Título: Perfil de empreendedor
     * Objetivo: Onboarding pelo cadastro da startup.
     * Decisões: Record imutável, sem estado.
     */
    record Empreendedor() implements PerfilDePapel {

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 24/11/2026
         * Hora: 01:00
         * Cartão Jira: KAN-12
         * Título: Papel do empreendedor
         * Objetivo: Devolver EMPREENDEDOR.
         * Decisões: Fixo.
         */
        @Override
        public Papel papel() {
            return Papel.EMPREENDEDOR;
        }

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 24/11/2026
         * Hora: 01:00
         * Cartão Jira: KAN-12
         * Título: Onboarding do empreendedor
         * Objetivo: Rota do cadastro da startup.
         * Decisões: A tela chega com o KAN-21 (S4).
         */
        @Override
        public String rotaOnboarding() {
            return "/empreendedor/onboarding";
        }

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 24/11/2026
         * Hora: 01:00
         * Cartão Jira: KAN-12
         * Título: Confidencialidade do empreendedor
         * Objetivo: Não exigir o Termo de Confidencialidade.
         * Decisões: O empreendedor é quem libera o Nível 2, não quem acessa.
         */
        @Override
        public boolean exigeTermoDeConfidencialidade() {
            return false;
        }
    }
}

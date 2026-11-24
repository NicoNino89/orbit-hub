package br.com.orbithub.compartilhado.email;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 01:00
 * Cartão Jira: KAN-12
 * Título: Envio de e-mail transacional
 * Objetivo: Enviar e-mails da plataforma (confirmação de conta, redefinição de senha, avisos).
 * Decisões: Padrão Adapter: os módulos dependem só desta interface; o provedor (SMTP local com Mailpit,
 *           ou o serviço escolhido para a nuvem) fica numa implementação trocável por configuração.
 */
public interface EnvioDeEmail {

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:00
     * Cartão Jira: KAN-12
     * Título: Enviar mensagem
     * Objetivo: Entregar a mensagem ao destinatário.
     * Decisões: Falha de envio não derruba a operação de quem chamou: a implementação registra o erro e
     *           o usuário pode pedir o reenvio.
     */
    void enviar(MensagemEmail mensagem);

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:00
     * Cartão Jira: KAN-12
     * Título: Mensagem de e-mail
     * Objetivo: Destinatário, assunto, texto e um rótulo para o log.
     * Decisões: Texto simples (sem HTML) no MVP; o rótulo ("confirmacao_email") identifica o tipo no log
     *           sem registrar o endereço, que é dado pessoal.
     */
    record MensagemEmail(String para, String assunto, String texto, String rotulo) {

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 24/11/2026
         * Hora: 01:00
         * Cartão Jira: KAN-12
         * Título: Representação textual segura
         * Objetivo: Evitar endereço e links (com token) em logs por acidente.
         * Decisões: Mostra só o rótulo.
         */
        @Override
        public String toString() {
            return "MensagemEmail[" + rotulo + "]";
        }
    }
}

package br.com.orbithub.compartilhado.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 01:00
 * Cartão Jira: KAN-12
 * Título: Escolha do provedor de e-mail
 * Objetivo: Criar a implementação de envio conforme orbit.email.tipo.
 * Decisões: "smtp" usa o servidor configurado em spring.mail (Mailpit no ambiente local); "registro" só
 *           registra no log que um e-mail seria enviado, sem endereço nem conteúdo, e é o padrão enquanto o
 *           provedor da nuvem não for escolhido.
 */
@Configuration(proxyBeanMethods = false)
class EmailConfig {

    private static final Logger LOG = LoggerFactory.getLogger(EmailConfig.class);

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:00
     * Cartão Jira: KAN-12
     * Título: Implementação de envio
     * Objetivo: Devolver o adaptador de SMTP ou o de registro.
     * Decisões: Tipo desconhecido falha na inicialização, para o erro aparecer no deploy.
     */
    @Bean
    EnvioDeEmail envioDeEmail(@Value("${orbit.email.tipo}") String tipo,
                              @Value("${orbit.email.remetente}") String remetente,
                              ObjectProvider<JavaMailSender> smtp) {
        return switch (tipo) {
            case "smtp" -> new SmtpEnvioDeEmail(smtp.getObject(), remetente);
            case "registro" -> mensagem -> LOG.info("E-mail não enviado (provedor não configurado): {}", mensagem);
            default -> throw new IllegalStateException("orbit.email.tipo inválido: " + tipo);
        };
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:00
     * Cartão Jira: KAN-12
     * Título: Adaptador SMTP
     * Objetivo: Enviar a mensagem pelo JavaMailSender do Spring.
     * Decisões: Erro de envio vira log de alerta (sem o endereço), sem propagar.
     */
    record SmtpEnvioDeEmail(JavaMailSender remetenteSmtp, String remetente) implements EnvioDeEmail {

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 24/11/2026
         * Hora: 12:36
         * Cartão Jira: KAN-146
         * Título: Enviar pelo SMTP
         * Objetivo: Enviar a mensagem de texto simples sem derrubar quem chamou.
         * Decisões: Usa a tentativa e ignora o resultado (os fluxos de conta oferecem o reenvio).
         */
        @Override
        public void enviar(MensagemEmail mensagem) {
            tentarEnviar(mensagem);
        }

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 24/11/2026
         * Hora: 12:36
         * Cartão Jira: KAN-146
         * Título: Tentar enviar pelo SMTP
         * Objetivo: Montar e enviar a mensagem e dizer se o servidor aceitou.
         * Decisões: Remetente fixo da configuração ("não responda"); a falha vai para o log sem o conteúdo.
         */
        @Override
        public boolean tentarEnviar(MensagemEmail mensagem) {
            SimpleMailMessage email = new SimpleMailMessage();
            email.setFrom(remetente);
            email.setTo(mensagem.para());
            email.setSubject(mensagem.assunto());
            email.setText(mensagem.texto());
            try {
                remetenteSmtp.send(email);
                return true;
            } catch (MailException falha) {
                LOG.warn("Falha ao enviar e-mail {}: {}", mensagem, falha.getClass().getSimpleName());
                return false;
            }
        }
    }
}

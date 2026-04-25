package org.code.api.config;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

/**
 * Classe de configuração para validação e mensagens de erro.
 * Define os beans necessários para carregar mensagens de validação personalizadas.
 */
@Configuration
public class ValidationConfig {

    /**
     * Configura a fonte de mensagens para validação.
     * Carrega arquivos de mensagens localizados nos caminhos especificados.
     *
     * @return uma instância de {@link MessageSource} configurada com os arquivos de mensagens.
     */
    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource source = new ReloadableResourceBundleMessageSource();

        source.setBasenames(
                "classpath:messages/validation-user",
                "classpath:messages/validation-vehicle",
                "classpath:messages/validation-generics"
        );

        source.setDefaultEncoding("UTF-8");
        return source;
    }

    /**
     * Configura o validador para usar a fonte de mensagens personalizada.
     *
     * @return uma instância de {@link LocalValidatorFactoryBean} configurada com a fonte de mensagens.
     */
    @Bean
    public LocalValidatorFactoryBean getValidator() {
        LocalValidatorFactoryBean bean = new LocalValidatorFactoryBean();
        bean.setValidationMessageSource(messageSource());
        return bean;
    }
}

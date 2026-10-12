package org.code.api.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import org.code.api.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.servlet.view.xslt.XsltView;
import org.springframework.web.servlet.view.xslt.XsltViewResolver;

/**
 * Checks the loaded MVC surface against prerequisites of the reviewed SSE/XSLT advisories.
 * Dependency findings remain visible; these assertions do not declare the framework patched.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
class MvcSecuritySurfaceIT extends PostgresIntegrationTest {
  @Autowired ApplicationContext context;

  @Autowired
  @Qualifier("requestMappingHandlerMapping")
  RequestMappingHandlerMapping mappings;

  /** No application or auto-configured XSLT view/resolver is registered in the actual context. */
  @Test
  void loadedContextHasNoXsltRendering() {
    assertThat(context.getBeansOfType(XsltView.class)).isEmpty();
    assertThat(context.getBeansOfType(XsltViewResolver.class)).isEmpty();
    assertThat(context.getBeansOfType(View.class).values())
        .noneMatch(view -> view instanceof XsltView);
  }

  /** Application handlers use response bodies and expose no SSE or explicit view-returning path. */
  @Test
  void applicationHandlersDoNotRenderViewsOrStreamFragments() {
    var handlers =
        mappings.getHandlerMethods().entrySet().stream()
            .filter(
                entry -> entry.getValue().getBeanType().getPackageName().startsWith("org.code.api"))
            .toList();
    assertThat(handlers).isNotEmpty();
    for (var entry : handlers) {
      var handler = entry.getValue();
      assertThat(AnnotatedElementUtils.hasAnnotation(handler.getBeanType(), RestController.class))
          .as("Response-body controller: %s", handler)
          .isTrue();
      var returnType = handler.getReturnType().getParameterType();
      assertThat(
              View.class.isAssignableFrom(returnType)
                  || ModelAndView.class.isAssignableFrom(returnType))
          .as("Explicit view-returning handler: %s", handler)
          .isFalse();
      assertThat(handler.getReturnType().getGenericParameterType().getTypeName())
          .doesNotContain("SseEmitter", "ServerSentEvent", "FragmentsRendering");
      assertThat(entry.getKey().getProducesCondition().getProducibleMediaTypes())
          .noneMatch(type -> type.toString().startsWith("text/event-stream"));
    }
  }
}

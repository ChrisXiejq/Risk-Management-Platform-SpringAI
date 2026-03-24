package com.inovationbehavior.backend.ai.skills;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ClasspathSkillRegistryTest {

    @Test
    void shouldLoadSkillMetadataAndInstructionsFromMarkdown() throws Exception {
        ResourcePatternResolver resolver = mock(ResourcePatternResolver.class);
        String skillMd = """
                ---
                name: Retrieval Expert
                description: gather risk evidence
                ---
                Use retrieval workflow.
                """;
        Resource resource = new ByteArrayResource(skillMd.getBytes(StandardCharsets.UTF_8)) {
            @Override
            public java.net.URL getURL() throws java.io.IOException {
                return new java.net.URL("file:/tmp/skills/retrieval/SKILL.md");
            }
        };
        when(resolver.getResources(anyString())).thenReturn(new Resource[]{resource});

        ClasspathSkillRegistry registry = new ClasspathSkillRegistry("classpath:skills/*/SKILL.md", resolver);

        assertEquals(1, registry.listSkills().size());
        assertEquals("retrieval", registry.listSkills().get(0).id());
        Optional<String> instructions = registry.getInstructions("retrieval");
        assertTrue(instructions.isPresent());
        assertTrue(instructions.get().contains("retrieval workflow"));
    }
}


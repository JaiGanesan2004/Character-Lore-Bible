package service

import model.character.Character

object ExportService {

    fun characterToMarkdown(character: Character): String?{
        val relationships = RelationshipService.getRelationships(character.name)

        val sb = StringBuilder()

// --- Formatting the Markdown ---
        sb.append("# 📜 Character Dossier: ${character.name}\n\n")

        sb.append("## 👤 General Information\n")
        sb.append("- **Role:** ${character.role}\n")
        sb.append("- **Race:** ${character.race ?: "Unknown"}\n")
        sb.append("- **Archetype:** ${character.archetype ?: "N/A"}\n")
        sb.append("- **Age:** ${character.age ?: "Unknown"}\n")
        sb.append("- **Power Level:** ${character.powerLevel}\n\n")

        //Relationships part
        if(relationships.isEmpty()){
            sb.append("*Character has no recorded bonds.*\n\n")
        }else{
            relationships.forEach { relation ->
                sb.append("- **${relation.targetName}**: ${relation.description}\n")
            }
        }

        //Final Touch
        sb.append("*## 📖 Biography\n")
        sb.append("${character.lore ?: "No background lore provided yet."}\n\n")

        sb.append("---\n")
        sb.append("Generated on: ${java.time.LocalDate.now()}")

        return sb.toString()
    }
}
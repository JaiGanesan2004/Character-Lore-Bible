package service

import model.character.Character

object ExportService {

    fun characterToMarkdown(character: Character, userId: Int): String?{
        val sb = StringBuilder()

        val relationships = RelationshipService.getRelationships(character.id, userId)
        val abilities: List<String> = CharacterService.getAbilitiesByCharacterId(character.id)
        val feats = FeatServices.getFeatsByCharacterId(character.id, userId)
// --- Formatting the Markdown ---
        sb.append("# 📜 Character Dossier: ${character.name}\n\n")

        if(!character.imageUrl.isNullOrEmpty()){
            val fullImageUrl = "http://localhost:8080/${character.imageUrl}"
            sb.append("![Character Portrait]($fullImageUrl)\n\n")
        }

        sb.append("## 👤 General Information\n")
        sb.append("- **Role:** ${character.role}\n")
        sb.append("- **Race:** ${character.race ?: "Unknown"}\n")
        sb.append("- **Archetype:** ${character.archetype ?: "N/A"}\n")
        sb.append("- **Age:** ${character.age ?: "Unknown"}\n")
        sb.append("- **Power Level:** ${character.powerLevel}\n\n")

        //Abilities Part
        sb.append("## ⚡ Abilities\n")
        if(abilities.isEmpty()){
            sb.append("*No signature moves recorded in the Bible.*\n\n")
        } else{
            abilities.forEach { ability ->
                sb.append("- $ability\n")
            }
            sb.append("\n")
        }

        //Feats
        sb.append("## 🏆 Feats\n")

        if(feats.isEmpty())
            sb.append("*No feats recorded yet📉.*\n\n")
        else{
            feats.forEach { feat ->
                sb.append("### ${feat.category}\n")
                sb.append("${feat.description}\n\n")
            }
        }

        //Relationships Part
        sb.append("## 🤝 Relationships\n")
        if(relationships.isEmpty()){
            sb.append("*Character has no recorded bonds.*\n\n")
        }else{
            relationships.forEach { relation ->
                sb.append("- **${relation.targetName}**: ${relation.description}\n")
            }
        }


        //Final Touch
        sb.append("## 📖 Biography\n")
        sb.append("${character.lore ?: "No background lore provided yet."}\n\n")

        sb.append("---\n")
        sb.append("Generated on: ${java.time.LocalDate.now()}")

        return sb.toString()
    }
}
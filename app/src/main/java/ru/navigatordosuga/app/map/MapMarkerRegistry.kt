package ru.navigatordosuga.app.map

import android.content.Context
import android.graphics.BitmapFactory
import org.maplibre.android.maps.Style
import ru.navigatordosuga.app.R
import ru.navigatordosuga.app.model.ActivityMode
import ru.navigatordosuga.app.model.EventItem
import ru.navigatordosuga.app.model.GeoItem

object MapMarkerRegistry {
    private val drawableMap = linkedMapOf(
        "mushroom_porcini" to R.drawable.marker_v16_1_mushrooms_porcini,
        "mushroom_chanterelle" to R.drawable.marker_v16_1_mushrooms_chanterelle,
        "mushroom_aspen" to R.drawable.marker_v16_1_mushrooms_aspen_bolete,
        "mushroom_birch" to R.drawable.marker_v16_1_mushrooms_birch_bolete,
        "mushroom_butter" to R.drawable.marker_v16_1_mushrooms_slippery_jack,
        "mushroom_honey" to R.drawable.marker_v16_1_mushrooms_honey,
        "mushroom_morel" to R.drawable.marker_v16_1_mushrooms_morel,
        "mushroom_saffron" to R.drawable.marker_v16_1_mushrooms_saffron,
        "mushroom_generic" to R.drawable.marker_v16_1_mushrooms_generic,
        "fish_pike" to R.drawable.marker_v16_1_fish_pike,
        "fish_perch" to R.drawable.marker_v16_1_fish_perch,
        "fish_carp" to R.drawable.marker_v16_1_fish_carp,
        "fish_trout" to R.drawable.marker_v16_1_fish_trout,
        "fish_bream" to R.drawable.marker_v16_1_fish_bream,
        "fish_zander" to R.drawable.marker_v16_1_fish_zander,
        "fish_catfish" to R.drawable.marker_v16_1_fish_catfish,
        "fish_crucian" to R.drawable.marker_v16_1_fish_crucian,
        "fish_generic" to R.drawable.marker_v16_1_fish_generic,
        "place_water" to R.drawable.marker_v16_1_places_water,
        "place_waterfall" to R.drawable.marker_v16_1_places_waterfall,
        "place_mountain" to R.drawable.marker_v16_1_places_mountain,
        "place_estate" to R.drawable.marker_v16_1_places_estate,
        "place_architecture" to R.drawable.marker_v16_1_places_architecture,
        "place_forest" to R.drawable.marker_v16_1_places_forest,
        "place_geology" to R.drawable.marker_v16_1_places_geology,
        "place_panorama" to R.drawable.marker_v16_1_places_panorama,
        "place_park" to R.drawable.marker_v16_1_places_park,
        "place_trail" to R.drawable.marker_v16_1_places_trail,
        "place_monument" to R.drawable.marker_v16_1_places_monument,
        "place_bridge" to R.drawable.marker_v16_1_places_bridge,
        "place_nature" to R.drawable.marker_v16_1_places_nature,
        "history_battle" to R.drawable.marker_v16_1_history_battle,
        "history_1812" to R.drawable.marker_v16_1_history_y1812,
        "history_ww2" to R.drawable.marker_v16_1_history_ww2,
        "history_soviet" to R.drawable.marker_v16_1_history_soviet,
        "history_estate" to R.drawable.marker_v16_1_history_estate,
        "history_architecture" to R.drawable.marker_v16_1_history_architecture,
        "history_religion" to R.drawable.marker_v16_1_history_religion,
        "history_literature" to R.drawable.marker_v16_1_history_literature,
        "history_science" to R.drawable.marker_v16_1_history_science,
        "history_fortress" to R.drawable.marker_v16_1_history_fortress,
        "history_default" to R.drawable.marker_v16_1_history_default,
        "cinema" to R.drawable.marker_cinema_clapper,
        "event_concert" to R.drawable.marker_events_concert,
        "event_standup" to R.drawable.marker_events_standup,
        "event_festival" to R.drawable.marker_events_festival,
        "event_fair" to R.drawable.marker_events_fair,
        "event_theatre" to R.drawable.marker_events_theatre,
        "event_exhibition" to R.drawable.marker_events_exhibition,
        "event_meet" to R.drawable.marker_events_meet,
        "event_cinema" to R.drawable.marker_events_cinema,
        "event_lecture" to R.drawable.marker_events_lecture,
        "event_food" to R.drawable.marker_events_food,
        "event_party" to R.drawable.marker_events_party,
        "event_sport" to R.drawable.marker_events_sport,
        "event_family" to R.drawable.marker_events_family,
        "event_masterclass" to R.drawable.marker_events_masterclass,
        "event_auto" to R.drawable.marker_events_auto,
        "event_gaming" to R.drawable.marker_events_gaming,
        "event_tech" to R.drawable.marker_events_tech,
        "event_outdoor" to R.drawable.marker_events_outdoor,
        "event_other" to R.drawable.marker_events_other
    )
    fun install(context:Context,style:Style){ drawableMap.forEach { (key,res) -> style.addImage(key,BitmapFactory.decodeResource(context.resources,res)) } }
    fun icon(item:GeoItem):String = when(item.mode){
        ActivityMode.MUSHROOMS -> when(item.iconKey.lowercase()){
            "белые","белый","боровик" -> "mushroom_porcini"; "лисички","лисичка" -> "mushroom_chanterelle"; "подосиновики","подосиновик" -> "mushroom_aspen"; "подберёзовики","подберезовики","подберёзовик" -> "mushroom_birch"; "маслята","маслёнок","масленок" -> "mushroom_butter"; "опята","опёнок" -> "mushroom_honey"; "сморчки","сморчок" -> "mushroom_morel"; "рыжики","рыжик" -> "mushroom_saffron"; else -> "mushroom_generic"
        }
        ActivityMode.FISHING -> when(item.iconKey.lowercase()){ "щука"->"fish_pike";"окунь"->"fish_perch";"карп"->"fish_carp";"форель"->"fish_trout";"лещ"->"fish_bream";"судак"->"fish_zander";"сом"->"fish_catfish";"карась"->"fish_crucian";else->"fish_generic" }
        ActivityMode.BEAUTIFUL -> when(item.iconKey.lowercase()){ "water","lake","river","водоем","водоём"->"place_water";"waterfall"->"place_waterfall";"mountain","height"->"place_mountain";"estate"->"place_estate";"architecture"->"place_architecture";"forest"->"place_forest";"geology","quarry"->"place_geology";"panorama"->"place_panorama";"park"->"place_park";"trail"->"place_trail";"monument"->"place_monument";"bridge"->"place_bridge";else->"place_nature" }
        ActivityMode.CINEMA -> "cinema"
        ActivityMode.HISTORY -> when(item.iconKey.lowercase()){ "battle","military"->"history_battle";"1812"->"history_1812";"ww2"->"history_ww2";"ussr","soviet"->"history_soviet";"estate"->"history_estate";"architecture"->"history_architecture";"religion"->"history_religion";"literature"->"history_literature";"science"->"history_science";"fortress"->"history_fortress";else->"history_default" }
        ActivityMode.EVENTS -> "event_other"
    }
    fun icon(event:EventItem)="event_"+when(event.category.lowercase()){ "concert"->"concert";"standup"->"standup";"festival"->"festival";"fair"->"fair";"theatre"->"theatre";"exhibition"->"exhibition";"meet","blogger"->"meet";"cinema"->"cinema";"lecture"->"lecture";"food"->"food";"party"->"party";"sport"->"sport";"family"->"family";"masterclass"->"masterclass";"auto"->"auto";"gaming"->"gaming";"tech"->"tech";"outdoor"->"outdoor";else->"other"}
}

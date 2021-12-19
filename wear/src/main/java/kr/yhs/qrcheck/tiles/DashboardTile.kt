package kr.yhs.qrcheck.tiles

import androidx.wear.tiles.ColorBuilders.argb
import androidx.wear.tiles.DeviceParametersBuilders.DeviceParameters
import androidx.wear.tiles.DimensionBuilders.*
import androidx.wear.tiles.LayoutElementBuilders.*
import androidx.wear.tiles.ModifiersBuilders.Modifiers
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.ResourceBuilders.Resources
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import androidx.wear.tiles.TimelineBuilders.Timeline
import androidx.wear.tiles.TimelineBuilders.TimelineEntry
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.*
import kotlinx.coroutines.guava.future
import kr.yhs.qrcheck.tiles.LayoutBuilder.font
import kr.yhs.qrcheck.tiles.LayoutBuilder.text
import kr.yhs.qrcheck.tiles.TilesBuilder.background
import kr.yhs.qrcheck.tiles.TilesBuilder.border
import kr.yhs.qrcheck.tiles.TilesBuilder.corner
import kr.yhs.qrcheck.tiles.TilesBuilder.padding
import org.jsoup.Jsoup


class DashboardTile: TileService() {
    private val RESOURCES_VERSION = "1"
    private val serviceScope = CoroutineScope(Dispatchers.IO)

    private lateinit var deviceParameters: DeviceParameters

    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> = serviceScope.future {
        deviceParameters = requestParams.deviceParameters!!
        TileBuilders.Tile.Builder()
            .setResourcesVersion(RESOURCES_VERSION)
            .setFreshnessIntervalMillis(3600000) // 60 minutes
            .setTimeline(
                Timeline.Builder().addTimelineEntry(
                    TimelineEntry.Builder().setLayout(
                        Layout.Builder().setRoot(
                            getLayout()
                        ).build()
                    ).build()
                ).build()
            ).build()
    }

    override fun onResourcesRequest(requestParams: RequestBuilders.ResourcesRequest): ListenableFuture<Resources> = serviceScope.future {
        Resources.Builder()
            .setVersion(RESOURCES_VERSION)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    private fun getLayout() =
        Box.Builder().apply {
            setWidth(expand())
            setHeight(expand())

            addContent(
                getColumn(
                    "QRpass",
                    "아래의 버튼을 눌러 QR 체크인을 하세요."
                )
            )
        }.build()

    private fun getColumn(data1: String, data2: String) =
        Column.Builder().apply {
            addContent(
                text(
                    textString = data1,
                    fontStyles = font(
                        size = sp(16f),
                        widght = FONT_WEIGHT_NORMAL
                    )
                )
            )
            addContent(
                text(
                    textString = data2,
                    fontStyles = font(
                        size = sp(13f)
                    )
                )
            )
        }.build()
}

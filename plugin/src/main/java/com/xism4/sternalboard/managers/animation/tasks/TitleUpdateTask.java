package com.xism4.sternalboard.managers.animation.tasks;

import com.xism4.sternalboard.SternalBoard;
import com.xism4.sternalboard.SternalBoardHandler;
import com.xism4.sternalboard.managers.ScoreboardManager;
import com.xism4.sternalboard.managers.animation.AnimationManager;
import com.xism4.sternalboard.utils.TextUtils;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

public class TitleUpdateTask extends BukkitRunnable {
    private final String[] lines;
    private final AnimationManager animationManager;
    private final ScoreboardManager scoreboardManager = SternalBoard.getInstance().getScoreboardManager();
    int index;

    public TitleUpdateTask(AnimationManager animationManager, List<String> lines) {
        this.animationManager = animationManager;
        this.lines = lines.toArray(new String[0]);
        this.index = 0;
    }

    @Override
    public void run() {
        animationManager.setTitle(lines[index]);
        index++;

        if (index == lines.length) {
            index = 0;
        }

        for (SternalBoardHandler sb : scoreboardManager.getBoardsHandler().values()) {
            String line = TextUtils.processPlaceholders(sb.getPlayer(), animationManager.getTitle());
            sb.updateTitle(line);
        }
    }
}

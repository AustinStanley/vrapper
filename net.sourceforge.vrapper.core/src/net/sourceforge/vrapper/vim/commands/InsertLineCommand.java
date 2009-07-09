package net.sourceforge.vrapper.vim.commands;

import net.sourceforge.vrapper.platform.CursorService;
import net.sourceforge.vrapper.platform.TextContent;
import net.sourceforge.vrapper.utils.LineInformation;
import net.sourceforge.vrapper.utils.VimUtils;
import net.sourceforge.vrapper.vim.EditorAdaptor;

/**
 * Inserts a new line.
 *
 * @author Matthias Radig
 */
public final class InsertLineCommand extends CountIgnoringNonRepeatableCommand {
    public static enum Type {
        PRE_CURSOR {
            @Override
            void dumb(EditorAdaptor vim, LineInformation line, String indent) {
                TextContent p = vim.getModelContent();
                String newline = vim.getConfiguration().getNewLine();
                p.replace(line.getBeginOffset(), 0, indent + newline);
                vim.getCursorService().setPosition(
                        vim.getCursorService().newPositionForModelOffset(line.getBeginOffset() + indent.length()),
                        true);
            }

            @Override
            void smart(EditorAdaptor vim, LineInformation line) {
                TextContent p = vim.getModelContent();
                String newline = vim.getConfiguration().getNewLine();
                CursorService cs = vim.getCursorService();
                if (line.getNumber() != 0) {
                    int index = p.getLineInformation(line.getNumber()-1).getEndOffset();
                    p.smartInsert(index, newline);
                    cs.setPosition(cs.newPositionForModelOffset(
                            p.getLineInformation(line.getNumber())
                            .getEndOffset()), true);
                } else {
                    p.smartInsert(0, newline);
                    cs.setPosition(cs.newPositionForModelOffset(0), true);
                }
            }
        },
        POST_CURSOR {
            @Override
            void dumb(EditorAdaptor vim, LineInformation line, String indent) {
                TextContent p = vim.getModelContent();
                int begin = line.getEndOffset();
                if (line.getNumber() == p.getNumberOfLines()-1) {
                    // there is a character at the end offset, which belongs to the line
                    begin += 1;
                }
                String newline = vim.getConfiguration().getNewLine();
                p.replace(begin, 0, newline+indent);
                CursorService cursorService = vim.getCursorService();
                cursorService.setPosition(cursorService.newPositionForModelOffset(
                        begin+indent.length()+newline.length()), true);
            }

            @Override
            void smart(EditorAdaptor vim, LineInformation line) {
                String newline = vim.getConfiguration().getNewLine();
                TextContent p = vim.getModelContent();

                CursorService cs = vim.getCursorService();
                int begin = line.getEndOffset();
                p.smartInsert(begin, newline);
                cs.setPosition(cs.newPositionForModelOffset(p.getLineInformation(
                        line.getNumber() + 1).getEndOffset()), true);
            }

        };

        abstract void smart(EditorAdaptor vim, LineInformation line);

        abstract void dumb(EditorAdaptor vim, LineInformation line, String indent);

    }

    private final Type type;

    public InsertLineCommand(InsertLineCommand.Type type) {
        this.type = type;
    }

    public final void execute(EditorAdaptor vim) {
        TextContent p = vim.getModelContent();
        LineInformation line = p.getLineInformationOfOffset(vim.getCursorService().getPosition().getModelOffset());
        if (vim.getConfiguration().isSmartIndent()) {
            this.type.smart(vim, line);
        } else {
            String indent = vim.getConfiguration().isAutoIndent() ? VimUtils
                    .getIndent(vim.getModelContent(), line) : "";
            this.type.dumb(vim, line, indent);
        }
    }

}

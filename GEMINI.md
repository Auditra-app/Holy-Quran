# Custom Agent Directives

## 1. Feature Preservation & Scope Strictness
- **Do NOT modify existing features** unless explicitly requested by the user. Always preserve working features, layouts, and configurations.
- **Do NOT suggest features that are already implemented.** Always check the codebase thoroughness before recommending or adding any functionality.

## 2. Prompt Engineering & Multi-Requirement Execution Protocol
- **Engineer the Prompt**: Before execution, analyze and engineer the prompt to identify all functional and design requirements.
- **Task Breakdown List**: For any prompt containing multiple requirements or sub-tasks, always generate a clear, structured Task List first before making code edits.
- **Strict Adherence**: Execute tasks sequentially according to the task list without adding unrequested extra features.

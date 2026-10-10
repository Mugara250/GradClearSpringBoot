import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { useCurrentUser } from '../../auth/AuthContext'
import {
  Button,
  Card,
  ConfirmDelete,
  EmptyState,
  ErrorNotice,
  Field,
  FormActions,
  Input,
  Loading,
  Modal,
  PageHeader,
  Select,
  Tag,
} from '../../components/ui'
import { useAcademics, useDeleteAcademic, useSaveAcademic, type AcademicInput } from '../../lib/queries'
import type { Academic, AcademicLevel } from '../../lib/types'

// The hierarchy the backend enforces: PROGRAM (top, no parent) > FACULTY > DEPARTMENT.
const PARENT_LEVEL: Record<AcademicLevel, AcademicLevel | null> = {
  PROGRAM: null,
  FACULTY: 'PROGRAM',
  DEPARTMENT: 'FACULTY',
}
const CHILD_LEVEL: Record<AcademicLevel, AcademicLevel | null> = {
  PROGRAM: 'FACULTY',
  FACULTY: 'DEPARTMENT',
  DEPARTMENT: null,
}
const LEVEL_LABEL: Record<AcademicLevel, string> = { PROGRAM: 'Program', FACULTY: 'Faculty', DEPARTMENT: 'Department' }

type Editing = { academic: Academic | null; level: AcademicLevel; parentId: string | null }

export function AcademicsPage() {
  const user = useCurrentUser()
  const isAdmin = user.role === 'ADMIN'
  const academics = useAcademics()
  const remove = useDeleteAcademic()
  const [editing, setEditing] = useState<Editing | null>(null)
  const [deleting, setDeleting] = useState<Academic | null>(null)

  const all = academics.data ?? []
  const childrenOf = (id: string | null) =>
    all.filter((a) => a.parentId === id).sort((a, b) => a.name.localeCompare(b.name))

  const renderNode = (node: Academic, depth: number) => {
    const childLevel = CHILD_LEVEL[node.level]
    return (
      <li key={node.id}>
        <div
          className="flex flex-wrap items-center gap-2 rounded-md py-1.5 pr-2 hover:bg-sky-50/60"
          style={{ paddingLeft: `${depth * 1.5 + 0.5}rem` }}
        >
          <Tag>{LEVEL_LABEL[node.level]}</Tag>
          <span className="font-semibold text-slate-900">{node.name}</span>
          {node.level === 'DEPARTMENT' && (
            <span className="text-xs text-slate-600">{node.requiredCredits ?? 0} credits required</span>
          )}
          <span className="ml-auto flex gap-1">
            {childLevel && (
              <Button size="sm" variant="ghost" onClick={() => setEditing({ academic: null, level: childLevel, parentId: node.id })}>
                + {LEVEL_LABEL[childLevel]}
              </Button>
            )}
            <Button size="sm" variant="ghost" onClick={() => setEditing({ academic: node, level: node.level, parentId: node.parentId })}>
              Edit
            </Button>
            {isAdmin && (
              <Button size="sm" variant="ghost" className="text-red-700" onClick={() => setDeleting(node)}>
                Delete
              </Button>
            )}
          </span>
        </div>
        {childrenOf(node.id).length > 0 && <ul>{childrenOf(node.id).map((c) => renderNode(c, depth + 1))}</ul>}
      </li>
    )
  }

  return (
    <>
      <PageHeader
        title="Academic structure"
        subtitle="Programs contain faculties, and faculties contain departments. Students belong to a department."
        actions={<Button onClick={() => setEditing({ academic: null, level: 'PROGRAM', parentId: null })}>New program</Button>}
      />
      <Card>
        {academics.isLoading && <Loading />}
        {academics.error && <ErrorNotice error={academics.error} onRetry={() => void academics.refetch()} />}
        {academics.data &&
          (all.length === 0 ? (
            <EmptyState>No academic entries yet. Start with a program.</EmptyState>
          ) : (
            <ul className="-mx-2">{childrenOf(null).map((p) => renderNode(p, 0))}</ul>
          ))}
      </Card>

      {editing && <AcademicFormModal {...editing} all={all} onClose={() => setEditing(null)} />}
      {deleting && (
        <ConfirmDelete
          what={`${LEVEL_LABEL[deleting.level].toLowerCase()} "${deleting.name}"`}
          pending={remove.isPending}
          error={remove.error}
          onClose={() => {
            remove.reset()
            setDeleting(null)
          }}
          onConfirm={() => remove.mutate(deleting.id, { onSuccess: () => setDeleting(null) })}
        />
      )}
    </>
  )
}

function AcademicFormModal({ academic, level, parentId, all, onClose }: Editing & { all: Academic[]; onClose: () => void }) {
  const save = useSaveAcademic()
  const { register, handleSubmit, watch, formState } = useForm<{ name: string; level: AcademicLevel; parentId: string; requiredCredits: number }>({
    defaultValues: {
      name: academic?.name ?? '',
      level,
      parentId: parentId ?? '',
      requiredCredits: academic?.requiredCredits ?? 0,
    },
  })
  const chosenLevel = watch('level')
  const parentLevel = PARENT_LEVEL[chosenLevel]
  const parents = parentLevel ? all.filter((a) => a.level === parentLevel && a.id !== academic?.id) : []
  const err = formState.errors

  const onSubmit = handleSubmit((values) => {
    const body: AcademicInput = {
      name: values.name.trim(),
      level: values.level,
      parentId: parentLevel ? values.parentId || null : null,
      requiredCredits: values.level === 'DEPARTMENT' ? Number(values.requiredCredits) : 0,
    }
    save.mutate({ id: academic?.id, body }, { onSuccess: onClose })
  })

  return (
    <Modal title={academic ? `Edit ${LEVEL_LABEL[academic.level].toLowerCase()}` : `New ${LEVEL_LABEL[level].toLowerCase()}`} onClose={onClose}>
      <form onSubmit={onSubmit} className="space-y-4" noValidate>
        <Field label="Name" error={err.name?.message}>
          {(id) => <Input id={id} {...register('name', { required: 'Required' })} />}
        </Field>
        <Field label="Level">
          {(id) => (
            <Select id={id} {...register('level')}>
              <option value="PROGRAM">Program</option>
              <option value="FACULTY">Faculty</option>
              <option value="DEPARTMENT">Department</option>
            </Select>
          )}
        </Field>
        {parentLevel && (
          <Field label={`Parent ${LEVEL_LABEL[parentLevel].toLowerCase()}`} error={err.parentId?.message}>
            {(id) => (
              <Select id={id} {...register('parentId', { validate: (v) => !!v || `A ${chosenLevel.toLowerCase()} needs a parent ${parentLevel.toLowerCase()}` })}>
                <option value="">Select…</option>
                {parents.map((p) => (
                  <option key={p.id} value={p.id}>
                    {p.name}
                  </option>
                ))}
              </Select>
            )}
          </Field>
        )}
        {chosenLevel === 'DEPARTMENT' && (
          <Field
            label="Credits required to graduate"
            error={err.requiredCredits?.message}
            hint={academic ? 'Note: the server currently ignores this field when editing (only applied on create).' : undefined}
          >
            {(id) => (
              <Input id={id} type="number" min={0} {...register('requiredCredits', { required: 'Required', min: { value: 0, message: 'Must be ≥ 0' } })} />
            )}
          </Field>
        )}
        <ErrorNotice error={save.error} />
        <FormActions onCancel={onClose} pending={save.isPending} />
      </form>
    </Modal>
  )
}
